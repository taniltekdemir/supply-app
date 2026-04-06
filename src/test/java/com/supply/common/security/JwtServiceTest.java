package com.supply.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-for-junit-min-32-chars!";
    private static final long EXPIRATION = 86400000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION);
    }

    @Test
    void generateToken_whenCalled_thenReturnsNonNullToken() {
        UUID tenantId = UUID.randomUUID();
        String email = "test@example.com";

        String token = jwtService.generateToken(tenantId, email);

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractTenantId_whenValidToken_thenReturnsTenantId() {
        UUID tenantId = UUID.randomUUID();
        String token = jwtService.generateToken(tenantId, "test@example.com");

        UUID extracted = jwtService.extractTenantId(token);

        assertThat(extracted).isEqualTo(tenantId);
    }

    @Test
    void extractEmail_whenValidToken_thenReturnsEmail() {
        String email = "test@example.com";
        String token = jwtService.generateToken(UUID.randomUUID(), email);

        String extracted = jwtService.extractEmail(token);

        assertThat(extracted).isEqualTo(email);
    }

    @Test
    void isTokenValid_whenExpiredToken_thenReturnsFalse() {
        JwtService expiredService = new JwtService(SECRET, -1L);
        String token = expiredService.generateToken(UUID.randomUUID(), "test@example.com");

        boolean valid = jwtService.isTokenValid(token);

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValid_whenTamperedToken_thenReturnsFalse() {
        String token = jwtService.generateToken(UUID.randomUUID(), "test@example.com");
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        boolean valid = jwtService.isTokenValid(tampered);

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValid_whenValidToken_thenReturnsTrue() {
        String token = jwtService.generateToken(UUID.randomUUID(), "test@example.com");

        boolean valid = jwtService.isTokenValid(token);

        assertThat(valid).isTrue();
    }
}
