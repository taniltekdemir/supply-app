package com.supply.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class OrderItemResponse {

    private UUID id;
    private ProductResponse product;
    private BigDecimal quantity;
    private String notes;
}
