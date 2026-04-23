package com.supply.payment.controller;

import com.supply.common.response.ApiResponse;
import com.supply.payment.dto.AddDebtRequest;
import com.supply.payment.dto.AddPaymentRequest;
import com.supply.payment.dto.CustomerAccountResponse;
import com.supply.payment.dto.CustomerTransactionResponse;
import com.supply.payment.dto.DailySummaryResponse;
import com.supply.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/debt")
    public ResponseEntity<ApiResponse<CustomerTransactionResponse>> addDebt(
            @Valid @RequestBody AddDebtRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(paymentService.addDebt(request)));
    }

    @PostMapping("/payment")
    public ResponseEntity<ApiResponse<CustomerTransactionResponse>> addPayment(
            @Valid @RequestBody AddPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(paymentService.addPayment(request)));
    }

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<CustomerAccountResponse>>> getAllAccounts() {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getAllAccounts()));
    }

    @GetMapping("/accounts/{customerId}")
    public ResponseEntity<ApiResponse<CustomerAccountResponse>> getAccountByCustomer(
            @PathVariable UUID customerId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getAccountByCustomer(customerId)));
    }

    @GetMapping("/transactions/{customerId}")
    public ResponseEntity<ApiResponse<List<CustomerTransactionResponse>>> getTransactionsByCustomer(
            @PathVariable UUID customerId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getTransactionsByCustomer(customerId)));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<CustomerTransactionResponse>>> getTransactionsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getTransactionsByDate(date)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DailySummaryResponse>> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getDailySummary(date)));
    }
}
