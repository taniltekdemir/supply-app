package com.supply.procurement.controller;

import com.supply.common.response.ApiResponse;
import com.supply.procurement.dto.ProcurementSummary;
import com.supply.procurement.service.ProcurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/procurement")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ProcurementSummary>> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(procurementService.getDailySummary(date)));
    }

    @GetMapping("/consolidated")
    public ResponseEntity<ApiResponse<ProcurementSummary>> getConsolidatedList(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(procurementService.getConsolidatedList(date)));
    }
}