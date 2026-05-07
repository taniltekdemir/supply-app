package com.supply.procurement.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.order.entity.Order;
import com.supply.order.entity.OrderItem;
import com.supply.order.entity.Product;
import com.supply.order.repository.OrderItemRepository;
import com.supply.order.repository.OrderRepository;
import com.supply.order.service.ProductService;
import com.supply.pricing.entity.DailyPrice;
import com.supply.pricing.repository.DailyPriceRepository;
import com.supply.procurement.dto.ProcurementItemSummary;
import com.supply.procurement.dto.ProcurementSummary;
import com.supply.tenant.entity.Tenant;
import com.supply.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcurementService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final TenantRepository tenantRepository;
    private final ProductService productService;

    public ProcurementSummary getDailySummary(LocalDate date) {
        Tenant tenant = currentTenant();

        List<Order> openOrders = orderRepository
                .findAllByTenantAndOrderDate(tenant, date);

        List<OrderItem> allItems = openOrders.stream()
                .flatMap(order -> orderItemRepository.findAllByOrder(order).stream())
                .toList();

        // Ürün bazında quantity topla; ekleme sırasını koru
        Map<UUID, BigDecimal> quantityByProductId = new LinkedHashMap<>();
        Map<UUID, Product> productById = new LinkedHashMap<>();
        for (OrderItem item : allItems) {
            UUID productId = item.getProduct().getId();
            quantityByProductId.merge(productId, item.getQuantity(), BigDecimal::add);
            productById.putIfAbsent(productId, item.getProduct());
        }

        List<ProcurementItemSummary> items = quantityByProductId.entrySet().stream()
                .map(entry -> buildItemSummary(entry.getKey(), entry.getValue(),
                        productById.get(entry.getKey()), tenant, date))
                .toList();

        BigDecimal totalCost = items.stream()
                .map(ProcurementItemSummary::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ProcurementSummary.builder()
                .date(date)
                .items(items)
                .totalCost(totalCost)
                .build();
    }

    public ProcurementSummary getConsolidatedList(LocalDate date) {
        ProcurementSummary full = getDailySummary(date);

        List<ProcurementItemSummary> pricedItems = full.getItems().stream()
                .filter(ProcurementItemSummary::isHasPrice)
                .toList();

        BigDecimal totalCost = pricedItems.stream()
                .map(ProcurementItemSummary::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ProcurementSummary.builder()
                .date(date)
                .items(pricedItems)
                .totalCost(totalCost)
                .build();
    }

    private ProcurementItemSummary buildItemSummary(
            UUID productId, BigDecimal totalQuantity, Product product,
            Tenant tenant, LocalDate date) {

        Optional<DailyPrice> price = dailyPriceRepository
                .findByTenantAndProductAndDate(tenant, product, date);

        BigDecimal unitCost = price.map(DailyPrice::getUnitCost).orElse(BigDecimal.ZERO);
        BigDecimal totalCost = price
                .map(p -> totalQuantity.multiply(p.getUnitCost()))
                .orElse(BigDecimal.ZERO);

        return ProcurementItemSummary.builder()
                .product(productService.toResponse(product))
                .totalQuantity(totalQuantity)
                .unitCost(unitCost)
                .totalCost(totalCost)
                .hasPrice(price.isPresent())
                .build();
    }

    private Tenant currentTenant() {
        return tenantRepository.findById(TenantContext.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
    }
}