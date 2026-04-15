package com.supply.pricing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DailyPriceRequest {

    @NotNull
    private UUID productId;

    @NotNull
    private LocalDate date;

    @NotNull
    @Positive
    private BigDecimal unitCost;

    @NotNull
    @Positive
    private BigDecimal sellingPrice;
}
