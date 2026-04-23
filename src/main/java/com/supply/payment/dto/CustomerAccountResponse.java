package com.supply.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CustomerAccountResponse {
    private UUID customerId;
    private String customerName;
    private BigDecimal balance;
    private LocalDateTime lastUpdated;
}
