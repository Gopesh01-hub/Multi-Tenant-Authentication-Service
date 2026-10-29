package com.gopesh.identity.audit.service;

import com.gopesh.identity.audit.entity.AuditLog;
import com.gopesh.identity.audit.repository.AuditLogRepository;
import com.gopesh.identity.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * Captures request-scoped values (tenant ID, IP, user-agent) on the calling
 * thread and delegates async persistence to {@link AuditLogWriter}.
 *
 * <p>The split into two beans is intentional — Spring's {@code @Async} relies
 * on AOP proxies, which are bypassed on self-invocation within a single bean.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogWriter auditLogWriter;
    private final AuditLogRepository auditLogRepository;

    public void log(String action, String resourceType, String resourceId, Map<String, Object> details) {
        String tenantId = TenantContext.getTenantId();
        String ipAddress = null;
        String userAgent = null;

        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            ipAddress = extractClientIp(request);
            userAgent = request.getHeader("User-Agent");
        }

        auditLogWriter.persist(tenantId, action, resourceType, resourceId, ipAddress, userAgent, details);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(String tenantId, Pageable pageable) {
        return auditLogRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
