package com.gopesh.identity.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Extracts the tenant identifier from the {@code X-Tenant-ID} request header
 * and places it in {@link TenantContext} for downstream consumption.
 * Falls back to the configured default tenant when the header is absent.
 *
 * <p>Also pushes the tenant ID into the SLF4J MDC so every log line
 * carries the tenant context without explicit plumbing.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantConfigProperties properties;
    private final TenantConfigRegistry registry;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String tenantId = request.getHeader(properties.getHeaderName());

        if (tenantId == null || tenantId.isBlank()) {
            tenantId = properties.getDefaultTenant();
        }

        registry.getConfig(tenantId);

        TenantContext.setTenantId(tenantId);
        MDC.put("tenantId", tenantId);

        log.debug("Tenant context set: {}", tenantId);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        TenantContext.clear();
        MDC.remove("tenantId");
    }
}
