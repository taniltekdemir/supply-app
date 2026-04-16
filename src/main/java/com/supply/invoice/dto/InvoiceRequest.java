package com.supply.invoice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRequest {

    @NotNull
    private UUID customerId;

    @NotNull
    private LocalDate invoiceDate;
}
