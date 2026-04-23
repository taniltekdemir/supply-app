package com.supply.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class DailySummaryResponse {
    private LocalDate date;
    private BigDecimal totalSales;
    private BigDecimal totalCash;
    private BigDecimal totalBankTransfer;
    private BigDecimal totalCreditCard;
    private BigDecimal totalNewDebt;
    private BigDecimal totalCollected;
    private BigDecimal openDebtBalance;
}
