package com.supply.tenant.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AuthResponse {

    private String token;
    private UUID tenantId;
    private String email;
}
