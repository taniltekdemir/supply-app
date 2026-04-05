package com.supply.tenant.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.security.JwtService;
import com.supply.tenant.dto.AuthResponse;
import com.supply.tenant.dto.LoginRequest;
import com.supply.tenant.dto.RegisterRequest;
import com.supply.tenant.entity.Tenant;
import com.supply.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final TenantRepository tenantRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (tenantRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.TENANT_ALREADY_EXISTS);
        }

        Tenant tenant = Tenant.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        Tenant saved = tenantRepository.save(tenant);
        String token = jwtService.generateToken(saved.getId(), saved.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tenantId(saved.getId())
                .email(saved.getEmail())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Tenant tenant = tenantRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), tenant.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtService.generateToken(tenant.getId(), tenant.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tenantId(tenant.getId())
                .email(tenant.getEmail())
                .build();
    }
}
