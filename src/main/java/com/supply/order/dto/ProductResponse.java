package com.supply.order.dto;

import com.supply.order.entity.Unit;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProductResponse {

    private UUID id;
    private String name;
    private Unit unit;
    private String description;
}
