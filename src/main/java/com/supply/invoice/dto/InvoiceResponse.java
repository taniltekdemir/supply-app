package com.supply.invoice.dto;

import com.supply.invoice.entity.InvoiceStatus;
import com.supply.order.dto.CustomerResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class InvoiceResponse {

    private UUID id;
    private CustomerResponse customer;
    private LocalDate invoiceDate;
    private InvoiceStatus status;
    private List<InvoiceItemResponse> items;
    private BigDecimal totalAmount;
}
