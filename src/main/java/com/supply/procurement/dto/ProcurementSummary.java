package com.supply.procurement.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ProcurementSummary {

    private LocalDate date;
    private List<ProcurementItemSummary> items;
    private BigDecimal totalCost;
}