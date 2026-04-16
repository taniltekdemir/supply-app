package com.supply.procurement.dto;

import com.supply.order.dto.ProductResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProcurementItemSummary {

    private ProductResponse product;
    private BigDecimal totalQuantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private boolean hasPrice;
}