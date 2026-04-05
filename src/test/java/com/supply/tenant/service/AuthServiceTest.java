package com.supply.tenant.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.tenant.dto.AuthResponse;
import com.supply.tenant.dto.LoginRequest;
import com.supply.tenant.dto.RegisterRequest;
import com.supply.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class AuthServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private AuthService authService;

    @Autowired
    private TenantRepository tenantRepository;

    @BeforeEach
    void cleanUp() {
        tenantRepository.deleteAll();
    }

    @Test
    void register_whenValidRequest_thenReturnToken() {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "password123");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getTenantId()).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void register_whenEmailAlreadyExists_thenThrowBusinessException() {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "password123");
        authService.register(request);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.TENANT_ALREADY_EXISTS));
    }

    @Test
    void login_whenInvalidPassword_thenThrowBusinessException() {
        RegisterRequest registerRequest = new RegisterRequest("Test User", "test@example.com", "password123");
        authService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest("test@example.com", "wrongpassword");

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }
}
