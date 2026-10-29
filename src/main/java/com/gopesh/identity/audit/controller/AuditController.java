package com.gopesh.identity.audit.controller;

import com.gopesh.identity.audit.entity.AuditLog;
import com.gopesh.identity.audit.service.AuditService;
import com.gopesh.identity.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Compliance audit trail (Admin only)")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retrieve paginated audit logs for the current tenant")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(auditService.getAuditLogs(tenantId, pageable));
    }
}
