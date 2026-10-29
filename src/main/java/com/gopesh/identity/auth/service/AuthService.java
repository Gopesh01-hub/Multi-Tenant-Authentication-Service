package com.gopesh.identity.auth.service;

import com.gopesh.identity.audit.service.AuditService;
import com.gopesh.identity.auth.dto.LoginRequest;
import com.gopesh.identity.auth.dto.RefreshRequest;
import com.gopesh.identity.auth.dto.TokenResponse;
import com.gopesh.identity.exception.AccountLockedException;
import com.gopesh.identity.security.JwtProperties;
import com.gopesh.identity.security.JwtTokenProvider;
import com.gopesh.identity.tenant.TenantContext;
import com.gopesh.identity.token.entity.RefreshToken;
import com.gopesh.identity.token.service.RefreshTokenService;
import com.gopesh.identity.user.entity.User;
import com.gopesh.identity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Core authentication service handling login, token refresh, and logout.
 *
 * <p>Account lockout: after 5 consecutive failed attempts the account is
 * locked for 30 minutes. Successful login resets the counter. All auth
 * events are recorded via {@link AuditService}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String tenantId = TenantContext.getTenantId();

        User user = userRepository.findByEmailAndTenantId(request.getEmail(), tenantId)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        checkAccountLock(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedAttempt(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        resetFailedAttempts(user);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), tenantId, user.getRole().name());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, request.getDeviceInfo());

        auditService.log("LOGIN_SUCCESS", "USER", String.valueOf(user.getId()), null);

        return TokenResponse.of(
                accessToken,
                refreshToken.getToken(),
                jwtProperties.getAccessTokenExpiry() / 1000,
                user.getRole().name(),
                tenantId
        );
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken rotatedToken = refreshTokenService.validateAndRotate(request.getRefreshToken());
        User user = rotatedToken.getUser();

        String accessToken = tokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getTenantId(), user.getRole().name());

        auditService.log("TOKEN_REFRESH", "USER", String.valueOf(user.getId()), null);

        return TokenResponse.of(
                accessToken,
                rotatedToken.getToken(),
                jwtProperties.getAccessTokenExpiry() / 1000,
                user.getRole().name(),
                user.getTenantId()
        );
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
        auditService.log("LOGOUT", "SESSION", refreshToken, null);
    }

    private void checkAccountLock(User user) {
        if (user.isLocked()) {
            if (user.getLockExpiresAt() != null && LocalDateTime.now().isAfter(user.getLockExpiresAt())) {
                user.setLocked(false);
                user.setFailedAttempts(0);
                user.setLockExpiresAt(null);
                userRepository.save(user);
            } else {
                throw new AccountLockedException(
                        "Account is locked until " + user.getLockExpiresAt());
            }
        }
    }

    private void handleFailedAttempt(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLocked(true);
            user.setLockExpiresAt(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("Account locked for user {} after {} failed attempts", user.getEmail(), attempts);
            auditService.log("ACCOUNT_LOCKED", "USER", String.valueOf(user.getId()), null);
        }

        userRepository.save(user);
        auditService.log("LOGIN_FAILED", "USER", String.valueOf(user.getId()), null);
    }

    private void resetFailedAttempts(User user) {
        if (user.getFailedAttempts() > 0) {
            user.setFailedAttempts(0);
            user.setLocked(false);
            user.setLockExpiresAt(null);
        }
    }
}
