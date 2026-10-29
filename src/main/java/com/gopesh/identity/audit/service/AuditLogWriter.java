package com.gopesh.identity.audit.service;

import com.gopesh.identity.audit.entity.AuditLog;
import com.gopesh.identity.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Dedicated bean for async audit persistence. Separated from
 * {@link AuditService} so that Spring's AOP proxy can intercept
 * the {@code @Async} annotation — self-invocation within a single
 * bean silently skips proxy-based advice.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogWriter {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(String tenantId, String action, String resourceType,
                        String resourceId, String ipAddress, String userAgent,
                        Map<String, Object> details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .tenantId(tenantId != null ? tenantId : "system")
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .details(details)
                    .status("SUCCESS")
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit: {} on {}:{} for tenant {}", action, resourceType, resourceId, tenantId);
        } catch (Exception ex) {
            log.error("Failed to write audit log for action={}", action, ex);
        }
    }
}
