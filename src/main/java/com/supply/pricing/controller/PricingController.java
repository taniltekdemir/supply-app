package com.supply.pricing.controller;

import com.supply.common.response.ApiResponse;
import com.supply.pricing.dto.DailyPriceRequest;
import com.supply.pricing.dto.DailyPriceResponse;
import com.supply.pricing.service.PricingService;
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
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @PostMapping
    public ResponseEntity<ApiResponse<DailyPriceResponse>> createOrUpdate(
            @Valid @RequestBody DailyPriceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(pricingService.createOrUpdate(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyPriceResponse>>> getPricesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(pricingService.getPricesByDate(date)));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<DailyPriceResponse>> getPriceByProductAndDate(
            @PathVariable UUID productId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(pricingService.getPriceByProductAndDate(productId, date)));
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<DailyPriceResponse>>> getPricesInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(pricingService.getPricesInRange(start, end)));
    }
}
