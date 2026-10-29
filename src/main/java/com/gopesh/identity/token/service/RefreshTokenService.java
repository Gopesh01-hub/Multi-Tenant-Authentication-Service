package com.gopesh.identity.token.service;

import com.gopesh.identity.exception.TokenExpiredException;
import com.gopesh.identity.security.JwtProperties;
import com.gopesh.identity.tenant.TenantConfigProperties;
import com.gopesh.identity.tenant.TenantConfigRegistry;
import com.gopesh.identity.token.entity.RefreshToken;
import com.gopesh.identity.token.repository.RefreshTokenRepository;
import com.gopesh.identity.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Manages refresh token lifecycle with tenant-aware session limits.
 *
 * <p>When the active session count for a user exceeds the tenant's
 * configured {@code maxSessions}, the oldest tokens are automatically
 * revoked — providing implicit session management without a dedicated
 * session store.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final TenantConfigRegistry tenantRegistry;

    @Transactional
    public RefreshToken createRefreshToken(User user, String deviceInfo) {
        TenantConfigProperties.TenantConfig tenantConfig = tenantRegistry.getConfig(user.getTenantId());

        long activeSessions = refreshTokenRepository.countByUserIdAndRevokedFalse(user.getId());
        if (activeSessions >= tenantConfig.getMaxSessions()) {
            log.info("Max sessions ({}) reached for user {} — revoking all existing tokens",
                    tenantConfig.getMaxSessions(), user.getEmail());
            refreshTokenRepository.revokeAllByUserId(user.getId());
        }

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .tenantId(user.getTenantId())
                .deviceInfo(deviceInfo)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpiry() / 1000))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken validateAndRotate(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> new TokenExpiredException("Refresh token not found or already revoked"));

        if (refreshToken.isExpired()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new TokenExpiredException("Refresh token has expired");
        }

        // Rotate: revoke old, issue new
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return createRefreshToken(refreshToken.getUser(), refreshToken.getDeviceInfo());
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                    log.info("Revoked refresh token for user {}", rt.getUser().getEmail());
                });
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Revoked all refresh tokens for userId={}", userId);
    }
}
