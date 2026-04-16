package com.supply.invoice.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.invoice.dto.InvoiceItemRequest;
import com.supply.invoice.dto.InvoiceItemResponse;
import com.supply.invoice.dto.InvoiceRequest;
import com.supply.invoice.dto.InvoiceResponse;
import com.supply.invoice.entity.Invoice;
import com.supply.invoice.entity.InvoiceItem;
import com.supply.invoice.entity.InvoiceStatus;
import com.supply.invoice.repository.InvoiceItemRepository;
import com.supply.invoice.repository.InvoiceRepository;
import com.supply.order.entity.Order;
import com.supply.order.entity.OrderItem;
import com.supply.order.entity.Product;
import com.supply.order.repository.OrderItemRepository;
import com.supply.order.repository.OrderRepository;
import com.supply.order.service.CustomerService;
import com.supply.order.service.ProductService;
import com.supply.pricing.repository.DailyPriceRepository;
import com.supply.tenant.entity.Tenant;
import com.supply.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final TenantRepository tenantRepository;
    private final CustomerService customerService;
    private final ProductService productService;
    private final DailyPriceRepository dailyPriceRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public InvoiceResponse createInvoice(InvoiceRequest request) {
        Tenant tenant = currentTenant();
        var customer = customerService.findByIdOrThrow(request.getCustomerId());

        Invoice invoice = Invoice.builder()
                .tenant(tenant)
                .customer(customer)
                .invoiceDate(request.getInvoiceDate())
                .build();

        return toResponse(invoiceRepository.save(invoice));
    }

    public InvoiceResponse createInvoiceFromOrder(UUID orderId) {
        Tenant tenant = currentTenant();

        Order order = orderRepository.findByIdAndTenant(orderId, tenant)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        Invoice invoice = Invoice.builder()
                .tenant(tenant)
                .customer(order.getCustomer())
                .invoiceDate(order.getOrderDate())
                .build();

        Invoice saved = invoiceRepository.save(invoice);

        List<OrderItem> orderItems = orderItemRepository.findAllByOrder(order);
        for (OrderItem orderItem : orderItems) {
            Product product = orderItem.getProduct();
            BigDecimal unitPrice = resolvePrice(product, order.getOrderDate());

            InvoiceItem invoiceItem = InvoiceItem.builder()
                    .tenant(tenant)
                    .invoice(saved)
                    .product(product)
                    .quantity(orderItem.getQuantity())
                    .unitPrice(unitPrice)
                    .notes(orderItem.getNotes())
                    .build();

            invoiceItemRepository.save(invoiceItem);
        }

        return toResponse(saved);
    }

    public InvoiceItemResponse addItem(UUID invoiceId, InvoiceItemRequest request) {
        Invoice invoice = findOpenInvoiceOrThrow(invoiceId);
        Product product = productService.findByIdOrThrow(request.getProductId());
        BigDecimal unitPrice = resolvePrice(product, invoice.getInvoiceDate());

        InvoiceItem item = InvoiceItem.builder()
                .tenant(currentTenant())
                .invoice(invoice)
                .product(product)
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .notes(request.getNotes())
                .build();

        return toItemResponse(invoiceItemRepository.save(item));
    }

    public void removeItem(UUID invoiceId, UUID itemId) {
        findOpenInvoiceOrThrow(invoiceId);

        InvoiceItem item = invoiceItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_ITEM_NOT_FOUND));

        invoiceItemRepository.delete(item);
    }

    public InvoiceResponse closeInvoice(UUID invoiceId) {
        Invoice invoice = findOpenInvoiceOrThrow(invoiceId);
        invoice.close();
        return toResponse(invoiceRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getById(UUID id) {
        return toResponse(findByIdOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getByStatus(InvoiceStatus status) {
        return invoiceRepository.findAllByTenantAndStatus(currentTenant(), status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getByDate(LocalDate date) {
        return invoiceRepository.findAllByTenantAndInvoiceDate(currentTenant(), date)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getByDateRange(LocalDate start, LocalDate end) {
        return invoiceRepository.findAllByTenantAndInvoiceDateBetween(currentTenant(), start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Invoice findByIdOrThrow(UUID id) {
        return invoiceRepository.findByIdAndTenant(id, currentTenant())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND));
    }

    private Invoice findOpenInvoiceOrThrow(UUID invoiceId) {
        Invoice invoice = findByIdOrThrow(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.CLOSED) {
            throw new BusinessException(ErrorCode.INVOICE_ALREADY_CLOSED);
        }
        return invoice;
    }

    private BigDecimal resolvePrice(Product product, LocalDate date) {
        return dailyPriceRepository
                .findByTenantAndProductAndDate(currentTenant(), product, date)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICE_NOT_FOUND))
                .getSellingPrice();
    }

    private Tenant currentTenant() {
        return tenantRepository.getReferenceById(TenantContext.get());
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceItem> rawItems = invoiceItemRepository.findAllByInvoice(invoice);

        List<InvoiceItemResponse> itemResponses = rawItems.stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal total = rawItems.stream()
                .map(InvoiceItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .customer(customerService.toResponse(invoice.getCustomer()))
                .invoiceDate(invoice.getInvoiceDate())
                .status(invoice.getStatus())
                .items(itemResponses)
                .totalAmount(total)
                .build();
    }

    private InvoiceItemResponse toItemResponse(InvoiceItem item) {
        return InvoiceItemResponse.builder()
                .id(item.getId())
                .product(productService.toResponse(item.getProduct()))
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .notes(item.getNotes())
                .build();
    }
}
