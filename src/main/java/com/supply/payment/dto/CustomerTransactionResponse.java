package com.supply.payment.dto;

import com.supply.order.dto.CustomerResponse;
import com.supply.payment.entity.PaymentMethod;
import com.supply.payment.entity.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class CustomerTransactionResponse {
    private UUID id;
    private CustomerResponse customer;
    private TransactionType type;
    private BigDecimal amount;
    private LocalDate date;
    private PaymentMethod paymentMethod;
    private UUID invoiceId;
    private String notes;
}
