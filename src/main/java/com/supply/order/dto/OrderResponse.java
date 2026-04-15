package com.supply.order.dto;

import com.supply.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class OrderResponse {

    private UUID id;
    private CustomerResponse customer;
    private LocalDate orderDate;
    private OrderStatus status;
    private List<OrderItemResponse> items;
}
