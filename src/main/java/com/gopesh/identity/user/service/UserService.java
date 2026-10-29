package com.gopesh.identity.user.service;

import com.gopesh.identity.audit.service.AuditService;
import com.gopesh.identity.exception.ResourceNotFoundException;
import com.gopesh.identity.exception.UserAlreadyExistsException;
import com.gopesh.identity.tenant.TenantConfigProperties;
import com.gopesh.identity.tenant.TenantConfigRegistry;
import com.gopesh.identity.tenant.TenantContext;
import com.gopesh.identity.user.dto.CreateUserRequest;
import com.gopesh.identity.user.dto.UserResponse;
import com.gopesh.identity.user.entity.User;
import com.gopesh.identity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User lifecycle management scoped to the current tenant.
 * Enforces the tenant's password policy during registration
 * and logs every mutation through the audit service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantConfigRegistry tenantRegistry;
    private final AuditService auditService;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String tenantId = TenantContext.getTenantId();

        if (userRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
            throw new UserAlreadyExistsException(request.getEmail(), tenantId);
        }

        TenantConfigProperties.TenantConfig tenantConfig = tenantRegistry.getConfig(tenantId);
        validatePasswordPolicy(request.getPassword(), tenantConfig.getPasswordPolicy());

        User user = User.builder()
                .tenantId(tenantId)
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(User.Role.USER)
                .build();

        User saved = userRepository.save(user);
        log.info("Created user {} in tenant {}", saved.getEmail(), tenantId);

        auditService.log("USER_CREATED", "USER", String.valueOf(saved.getId()), null);

        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        return userRepository.findAllByTenantId(tenantId, pageable)
                .map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .filter(u -> u.getTenantId().equals(TenantContext.getTenantId()))
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return UserResponse.from(user);
    }

    private void validatePasswordPolicy(String password, TenantConfigProperties.PasswordPolicy policy) {
        if (password.length() < policy.getMinLength()) {
            throw new IllegalArgumentException(
                    "Password must be at least " + policy.getMinLength() + " characters");
        }
        if (policy.isRequireUppercase() && password.equals(password.toLowerCase())) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (policy.isRequireSpecialChar() && password.matches("^[a-zA-Z0-9]*$")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }
}
