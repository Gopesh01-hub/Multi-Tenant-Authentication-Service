package com.gopesh.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * Stateless JWT token issuer and validator.
 *
 * <p>Tokens carry the tenant ID and user role as claims, enabling
 * downstream components to enforce tenant isolation and RBAC
 * without additional database lookups.</p>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String email, String tenantId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpiry());

        return Jwts.builder()
                .claim("email", email)
                .claim("tenantId", tenantId)
                .claim("role", role)
                .subject(String.valueOf(userId))
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public Claims validateAndExtract(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            log.debug("Token expired for subject: {}", ex.getClaims().getSubject());
            throw ex;
        }
    }

    public String extractSubject(String token) {
        return validateAndExtract(token).getSubject();
    }

    public String extractTenantId(String token) {
        return validateAndExtract(token).get("tenantId", String.class);
    }

    public String extractRole(String token) {
        return validateAndExtract(token).get("role", String.class);
    }
}
