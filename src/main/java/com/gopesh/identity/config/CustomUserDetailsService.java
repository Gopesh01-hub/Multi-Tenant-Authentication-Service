package com.gopesh.identity.config;

import com.gopesh.identity.security.UserPrincipal;
import com.gopesh.identity.tenant.TenantContext;
import com.gopesh.identity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Tenant-aware {@link UserDetailsService} that resolves users within
 * the scope of the current tenant, preventing cross-tenant lookups.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String tenantId = TenantContext.getTenantId();
        return userRepository.findByEmailAndTenantId(email, tenantId)
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + email + " in tenant: " + tenantId));
    }
}
