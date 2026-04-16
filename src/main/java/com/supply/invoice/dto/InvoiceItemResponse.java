package com.supply.invoice.dto;

import com.supply.order.dto.ProductResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class InvoiceItemResponse {

    private UUID id;
    private ProductResponse product;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private String notes;
}
