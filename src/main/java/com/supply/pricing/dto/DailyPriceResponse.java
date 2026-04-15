package com.supply.pricing.dto;

import com.supply.order.dto.ProductResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class DailyPriceResponse {

    private UUID id;
    private ProductResponse product;
    private LocalDate date;
    private BigDecimal unitCost;
    private BigDecimal sellingPrice;
    private BigDecimal profitMargin;
}
