package com.supply.pricing.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.order.service.ProductService;
import com.supply.pricing.dto.DailyPriceRequest;
import com.supply.pricing.dto.DailyPriceResponse;
import com.supply.pricing.entity.DailyPrice;
import com.supply.pricing.repository.DailyPriceRepository;
import com.supply.tenant.entity.Tenant;
import com.supply.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PricingService {

    private final DailyPriceRepository dailyPriceRepository;
    private final TenantRepository tenantRepository;
    private final ProductService productService;

    public DailyPriceResponse createOrUpdate(DailyPriceRequest request) {
        Tenant tenant = currentTenant();
        var product = productService.findByIdOrThrow(request.getProductId());

        Optional<DailyPrice> existing = dailyPriceRepository
                .findByTenantAndProductAndDate(tenant, product, request.getDate());

        DailyPrice saved;
        if (existing.isPresent()) {
            DailyPrice price = existing.get();
            price.updatePrices(request.getUnitCost(), request.getSellingPrice());
            saved = dailyPriceRepository.save(price);
        } else {
            DailyPrice price = DailyPrice.builder()
                    .tenant(tenant)
                    .product(product)
                    .date(request.getDate())
                    .unitCost(request.getUnitCost())
                    .sellingPrice(request.getSellingPrice())
                    .build();
            saved = dailyPriceRepository.save(price);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DailyPriceResponse> getPricesByDate(LocalDate date) {
        return dailyPriceRepository.findAllByTenantAndDate(currentTenant(), date)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DailyPriceResponse getPriceByProductAndDate(UUID productId, LocalDate date) {
        var product = productService.findByIdOrThrow(productId);
        DailyPrice price = dailyPriceRepository
                .findByTenantAndProductAndDate(currentTenant(), product, date)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICE_NOT_FOUND));
        return toResponse(price);
    }

    @Transactional(readOnly = true)
    public List<DailyPriceResponse> getPricesInRange(LocalDate start, LocalDate end) {
        return dailyPriceRepository.findAllByTenantAndDateBetween(currentTenant(), start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Tenant currentTenant() {
        return tenantRepository.getReferenceById(TenantContext.get());
    }

    private DailyPriceResponse toResponse(DailyPrice price) {
        return DailyPriceResponse.builder()
                .id(price.getId())
                .product(productService.toResponse(price.getProduct()))
                .date(price.getDate())
                .unitCost(price.getUnitCost())
                .sellingPrice(price.getSellingPrice())
                .profitMargin(calculateProfitMargin(price.getUnitCost(), price.getSellingPrice()))
                .build();
    }

    private BigDecimal calculateProfitMargin(BigDecimal unitCost, BigDecimal sellingPrice) {
        if (unitCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return sellingPrice.subtract(unitCost)
                .divide(unitCost, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
