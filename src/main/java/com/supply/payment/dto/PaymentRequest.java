package com.supply.payment.dto;

import com.supply.payment.entity.PaymentMethod;
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
public class PaymentRequest {

    @NotNull
    private UUID invoiceId;

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDate paymentDate;
}
