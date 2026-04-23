package com.supply.payment.dto;

import com.supply.payment.entity.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class PaymentResponse {

    private UUID id;
    private UUID invoiceId;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private boolean isPaid;
    private LocalDate paymentDate;
}
