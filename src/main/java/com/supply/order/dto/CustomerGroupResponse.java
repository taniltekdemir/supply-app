package com.supply.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CustomerGroupResponse {

    private UUID id;
    private String name;
    private String description;
    private String color;
    private long customerCount;
}
