package com.supply.invoice.controller;

import com.supply.common.response.ApiResponse;
import com.supply.invoice.dto.InvoiceItemRequest;
import com.supply.invoice.dto.InvoiceItemResponse;
import com.supply.invoice.dto.InvoiceRequest;
import com.supply.invoice.dto.InvoiceResponse;
import com.supply.invoice.entity.InvoiceStatus;
import com.supply.invoice.service.InvoicePdfService;
import com.supply.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(
            @Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(invoiceService.createInvoice(request)));
    }

    @PostMapping("/from-order/{orderId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoiceFromOrder(
            @PathVariable UUID orderId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(invoiceService.createInvoiceFromOrder(orderId)));
    }

    @GetMapping("/open")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getOpenInvoices() {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.getByStatus(InvoiceStatus.OPEN)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.getByDate(date)));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ApiResponse<InvoiceItemResponse>> addItem(
            @PathVariable UUID id,
            @Valid @RequestBody InvoiceItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(invoiceService.addItem(id, request)));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId) {
        invoiceService.removeItem(id, itemId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(@PathVariable UUID id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponse<InvoiceResponse>> closeInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.closeInvoice(id)));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        InvoiceResponse invoice = invoiceService.getById(id);
        byte[] pdf = invoicePdfService.generate(invoice);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"invoice-" + id + ".pdf\"")
                .body(pdf);
    }
}
