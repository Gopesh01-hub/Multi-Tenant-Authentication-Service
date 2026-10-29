package com.gopesh.identity.audit.repository;

import com.gopesh.identity.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    List<AuditLog> findByUserIdAndTenantId(Long userId, String tenantId);

    Page<AuditLog> findByTenantIdAndActionAndCreatedAtBetween(
            String tenantId, String action,
            LocalDateTime from, LocalDateTime to,
            Pageable pageable);

    long countByTenantIdAndAction(String tenantId, String action);
}
