package com.supply.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class PaymentSummaryResponse {

    private LocalDate date;
    private BigDecimal cashTotal;
    private BigDecimal transferTotal;
    private BigDecimal creditTotal;
    /** Gerçekten tahsil edilen tutar: nakit + transfer */
    private BigDecimal collectedTotal;
    private List<PaymentResponse> payments;
}
