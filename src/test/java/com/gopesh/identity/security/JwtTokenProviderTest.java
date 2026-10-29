package com.gopesh.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW11c3QtYmUtMzItYnl0ZXM=");
        properties.setAccessTokenExpiry(60_000);
        properties.setRefreshTokenExpiry(300_000);
        properties.setIssuer("test-identity");
        tokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    @DisplayName("Should generate a valid JWT and extract all claims")
    void shouldGenerateAndValidateToken() {
        String token = tokenProvider.generateAccessToken(1L, "user@test.com", "sky-uk", "USER");

        Claims claims = tokenProvider.validateAndExtract(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("email", String.class)).isEqualTo("user@test.com");
        assertThat(claims.get("tenantId", String.class)).isEqualTo("sky-uk");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getIssuer()).isEqualTo("test-identity");
    }

    @Test
    @DisplayName("Should extract subject from token")
    void shouldExtractSubject() {
        String token = tokenProvider.generateAccessToken(42L, "admin@test.com", "rogers-ca", "ADMIN");

        assertThat(tokenProvider.extractSubject(token)).isEqualTo("42");
    }

    @Test
    @DisplayName("Should extract tenant ID from token")
    void shouldExtractTenantId() {
        String token = tokenProvider.generateAccessToken(1L, "user@test.com", "sky-uk", "USER");

        assertThat(tokenProvider.extractTenantId(token)).isEqualTo("sky-uk");
    }

    @Test
    @DisplayName("Should extract role from token")
    void shouldExtractRole() {
        String token = tokenProvider.generateAccessToken(1L, "user@test.com", "sky-uk", "ADMIN");

        assertThat(tokenProvider.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should throw ExpiredJwtException for an expired token")
    void shouldRejectExpiredToken() {
        JwtProperties shortExpiry = new JwtProperties();
        shortExpiry.setSecret("dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW11c3QtYmUtMzItYnl0ZXM=");
        shortExpiry.setAccessTokenExpiry(0);
        shortExpiry.setIssuer("test-identity");
        JwtTokenProvider shortProvider = new JwtTokenProvider(shortExpiry);

        String token = shortProvider.generateAccessToken(1L, "user@test.com", "sky-uk", "USER");

        assertThatThrownBy(() -> shortProvider.validateAndExtract(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Should reject a tampered token")
    void shouldRejectTamperedToken() {
        String token = tokenProvider.generateAccessToken(1L, "user@test.com", "sky-uk", "USER");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> tokenProvider.validateAndExtract(tampered))
                .isInstanceOf(Exception.class);
    }
}
