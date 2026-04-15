package com.supply.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CustomerResponse {

    private UUID id;
    private String name;
    private String phone;
    private String address;
    private String notes;
    private LocalDateTime createdAt;
}
