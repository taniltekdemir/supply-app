package com.supply.invoice.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.invoice.dto.InvoiceItemRequest;
import com.supply.invoice.dto.InvoiceItemResponse;
import com.supply.invoice.dto.InvoiceRequest;
import com.supply.invoice.dto.InvoiceResponse;
import com.supply.invoice.entity.InvoiceStatus;
import com.supply.invoice.repository.InvoiceItemRepository;
import com.supply.invoice.repository.InvoiceRepository;
import com.supply.order.dto.CustomerRequest;
import com.supply.order.dto.OrderItemRequest;
import com.supply.order.dto.OrderRequest;
import com.supply.order.dto.ProductRequest;
import com.supply.order.entity.Unit;
import com.supply.order.repository.CustomerRepository;
import com.supply.order.repository.OrderItemRepository;
import com.supply.order.repository.OrderRepository;
import com.supply.order.repository.ProductRepository;
import com.supply.order.service.CustomerService;
import com.supply.order.service.OrderService;
import com.supply.order.service.ProductService;
import com.supply.pricing.dto.DailyPriceRequest;
import com.supply.pricing.repository.DailyPriceRepository;
import com.supply.pricing.service.PricingService;
import com.supply.tenant.dto.RegisterRequest;
import com.supply.tenant.repository.TenantRepository;
import com.supply.tenant.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.supply.supply_app.SupplyAppApplication.class)
@Testcontainers
class InvoiceServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ProductService productService;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private DailyPriceRepository dailyPriceRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AuthService authService;

    private UUID customerId;
    private UUID productId;
    private static final LocalDate TEST_DATE = LocalDate.of(2026, 4, 16);

    @BeforeEach
    void setUp() {
        var auth = authService.register(new RegisterRequest("Invoice Tenant", "invoice@test.com", "password123"));
        TenantContext.set(auth.getTenantId());

        customerId = customerService.create(new CustomerRequest("Test Müşteri", "555-0000", null, null, null)).getId();
        productId = productService.create(new ProductRequest("Domates", Unit.KG, null)).getId();
        pricingService.createOrUpdate(new DailyPriceRequest(
                productId, TEST_DATE,
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(12.50)));
    }

    @AfterEach
    void tearDown() {
        invoiceItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        dailyPriceRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();
        tenantRepository.deleteAll();
        TenantContext.clear();
    }

    @Test
    void createInvoice_whenValidRequest_thenReturnsOpenInvoice() {
        InvoiceResponse response = invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE));

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.OPEN);
        assertThat(response.getInvoiceDate()).isEqualTo(TEST_DATE);
        assertThat(response.getCustomer().getId()).isEqualTo(customerId);
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void addItem_whenPriceExists_thenItemAdded() {
        InvoiceResponse invoice = invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE));

        InvoiceItemResponse item = invoiceService.addItem(
                invoice.getId(),
                new InvoiceItemRequest(productId, BigDecimal.valueOf(5.5), "taze olsun"));

        assertThat(item.getId()).isNotNull();
        assertThat(item.getProduct().getId()).isEqualTo(productId);
        assertThat(item.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(5.5));
        assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(12.50));
        assertThat(item.getLineTotal()).isEqualByComparingTo(BigDecimal.valueOf(68.75));
        assertThat(item.getNotes()).isEqualTo("taze olsun");
    }

    @Test
    void getById_afterAddingItem_thenTotalAmountCorrect() {
        InvoiceResponse invoice = invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE));
        invoiceService.addItem(invoice.getId(), new InvoiceItemRequest(productId, BigDecimal.valueOf(5.5), null));

        InvoiceResponse result = invoiceService.getById(invoice.getId());

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(68.75));
    }

    @Test
    void removeItem_whenInvoiceOpen_thenItemRemoved() {
        InvoiceResponse invoice = invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE));
        InvoiceItemResponse item = invoiceService.addItem(
                invoice.getId(), new InvoiceItemRequest(productId, BigDecimal.valueOf(2.0), null));

        invoiceService.removeItem(invoice.getId(), item.getId());

        assertThat(invoiceService.getById(invoice.getId()).getItems()).isEmpty();
    }

    @Test
    void closeInvoice_whenOpen_thenStatusIsClosed() {
        InvoiceResponse invoice = invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE));

        InvoiceResponse closed = invoiceService.closeInvoice(invoice.getId());

        assertThat(closed.getStatus()).isEqualTo(InvoiceStatus.CLOSED);
    }

    @Test
    void getByStatus_whenOpenAndClosed_thenFiltersCorrectly() {
        invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE));
        InvoiceResponse second = invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE));
        invoiceService.closeInvoice(second.getId());

        List<InvoiceResponse> open = invoiceService.getByStatus(InvoiceStatus.OPEN);
        List<InvoiceResponse> closed = invoiceService.getByStatus(InvoiceStatus.CLOSED);

        assertThat(open).hasSize(1);
        assertThat(closed).hasSize(1);
        assertThat(open.get(0).getStatus()).isEqualTo(InvoiceStatus.OPEN);
        assertThat(closed.get(0).getStatus()).isEqualTo(InvoiceStatus.CLOSED);
    }

    @Test
    void getByDate_thenReturnsOnlyMatchingInvoices() {
        invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE));
        invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE));
        invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE.plusDays(1)));

        List<InvoiceResponse> result = invoiceService.getByDate(TEST_DATE);

        assertThat(result).hasSize(2);
    }

    @Test
    void closeInvoice_whenAlreadyClosed_thenThrowsInvoiceAlreadyClosed() {
        InvoiceResponse invoice = invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE));
        invoiceService.closeInvoice(invoice.getId());

        assertThatThrownBy(() -> invoiceService.closeInvoice(invoice.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVOICE_ALREADY_CLOSED));
    }

    @Test
    void addItem_whenInvoiceClosed_thenThrowsInvoiceAlreadyClosed() {
        InvoiceResponse invoice = invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE));
        invoiceService.closeInvoice(invoice.getId());

        assertThatThrownBy(() -> invoiceService.addItem(
                invoice.getId(), new InvoiceItemRequest(productId, BigDecimal.valueOf(1.0), null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVOICE_ALREADY_CLOSED));
    }

    @Test
    void addItem_whenPriceNotFound_thenThrowsPriceNotFound() {
        LocalDate dateWithoutPrice = LocalDate.of(2026, 4, 17);
        InvoiceResponse invoice = invoiceService.createInvoice(new InvoiceRequest(customerId, dateWithoutPrice));

        assertThatThrownBy(() -> invoiceService.addItem(
                invoice.getId(), new InvoiceItemRequest(productId, BigDecimal.valueOf(1.0), null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PRICE_NOT_FOUND));
    }

    @Test
    void removeItem_whenInvoiceClosed_thenThrowsInvoiceAlreadyClosed() {
        InvoiceResponse invoice = invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE));
        InvoiceItemResponse item = invoiceService.addItem(
                invoice.getId(), new InvoiceItemRequest(productId, BigDecimal.valueOf(1.0), null));
        invoiceService.closeInvoice(invoice.getId());

        assertThatThrownBy(() -> invoiceService.removeItem(invoice.getId(), item.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVOICE_ALREADY_CLOSED));
    }

    @Test
    void getById_whenNotFound_thenThrowsInvoiceNotFound() {
        assertThatThrownBy(() -> invoiceService.getById(UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVOICE_NOT_FOUND));
    }

    @Test
    void getById_whenDifferentTenant_thenThrowsInvoiceNotFound() {
        InvoiceResponse invoice = invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE));
        UUID invoiceId = invoice.getId();

        var auth2 = authService.register(new RegisterRequest("Other Tenant", "other@test.com", "password123"));
        TenantContext.set(auth2.getTenantId());

        assertThatThrownBy(() -> invoiceService.getById(invoiceId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVOICE_NOT_FOUND));
    }

    @Test
    void createInvoiceFromOrder_whenPriceExists_thenCreatesItemsAutomatically() {
        var order = orderService.createOrder(new OrderRequest(customerId, TEST_DATE));
        orderService.addItemToOrder(order.getId(), new OrderItemRequest(productId, BigDecimal.valueOf(3.0), "sipariş notu"));

        InvoiceResponse invoice = invoiceService.createInvoiceFromOrder(order.getId());

        assertThat(invoice.getId()).isNotNull();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.OPEN);
        assertThat(invoice.getInvoiceDate()).isEqualTo(TEST_DATE);
        assertThat(invoice.getCustomer().getId()).isEqualTo(customerId);
        assertThat(invoice.getItems()).hasSize(1);
        assertThat(invoice.getItems().get(0).getProduct().getId()).isEqualTo(productId);
        assertThat(invoice.getItems().get(0).getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(3.0));
        assertThat(invoice.getItems().get(0).getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(12.50));
        assertThat(invoice.getItems().get(0).getNotes()).isEqualTo("sipariş notu");
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(37.50));
    }

    @Test
    void createInvoiceFromOrder_whenPriceMissing_thenThrowsPriceNotFound() {
        LocalDate dateWithoutPrice = LocalDate.of(2026, 4, 17);
        var order = orderService.createOrder(new OrderRequest(customerId, dateWithoutPrice));
        orderService.addItemToOrder(order.getId(), new OrderItemRequest(productId, BigDecimal.valueOf(1.0), null));

        assertThatThrownBy(() -> invoiceService.createInvoiceFromOrder(order.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PRICE_NOT_FOUND));
    }

    @Test
    void deleteInvoice_whenOpen_thenInvoiceDeleted() {
        InvoiceResponse invoice = invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE));
        invoiceService.addItem(invoice.getId(), new InvoiceItemRequest(productId, BigDecimal.valueOf(2.0), null));

        invoiceService.deleteInvoice(invoice.getId());

        assertThatThrownBy(() -> invoiceService.getById(invoice.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVOICE_NOT_FOUND));
    }

    @Test
    void deleteInvoice_whenClosed_thenThrowsInvoiceAlreadyClosed() {
        InvoiceResponse invoice = invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE));
        invoiceService.closeInvoice(invoice.getId());

        assertThatThrownBy(() -> invoiceService.deleteInvoice(invoice.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVOICE_ALREADY_CLOSED));
    }

    @Test
    void createInvoiceFromOrder_whenAlreadyExists_thenThrowsInvoiceAlreadyExists() {
        var order = orderService.createOrder(new OrderRequest(customerId, TEST_DATE));
        orderService.addItemToOrder(order.getId(), new OrderItemRequest(productId, BigDecimal.valueOf(1.0), null));
        invoiceService.createInvoiceFromOrder(order.getId());

        assertThatThrownBy(() -> invoiceService.createInvoiceFromOrder(order.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVOICE_ALREADY_EXISTS));
    }
}
