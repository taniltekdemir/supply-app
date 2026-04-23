package com.supply.payment.controller;

import com.supply.common.response.ApiResponse;
import com.supply.payment.dto.PaymentRequest;
import com.supply.payment.dto.PaymentResponse;
import com.supply.payment.dto.PaymentSummaryResponse;
import com.supply.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(paymentService.createPayment(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getById(id)));
    }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getByInvoice(
            @PathVariable UUID invoiceId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getByInvoice(invoiceId)));
    }

    @PutMapping("/{id}/mark-paid")
    public ResponseEntity<ApiResponse<PaymentResponse>> markAsPaid(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.markAsPaid(id)));
    }

    @GetMapping("/unpaid")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getUnpaidCredits() {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getUnpaidCredits()));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PaymentSummaryResponse>> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getDailySummary(date)));
    }
}
