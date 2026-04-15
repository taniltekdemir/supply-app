package com.supply.order.controller;

import com.supply.common.response.ApiResponse;
import com.supply.order.dto.OrderItemRequest;
import com.supply.order.dto.OrderItemResponse;
import com.supply.order.dto.OrderRequest;
import com.supply.order.dto.OrderResponse;
import com.supply.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(orderService.createOrder(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrdersByDate(date)));
    }

    @GetMapping("/open")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOpenOrders() {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOpenOrders()));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ApiResponse<OrderItemResponse>> addItem(
            @PathVariable UUID id,
            @Valid @RequestBody OrderItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(orderService.addItemToOrder(id, request)));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId) {
        orderService.removeItemFromOrder(id, itemId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponse<OrderResponse>> closeOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.closeOrder(id)));
    }
}
