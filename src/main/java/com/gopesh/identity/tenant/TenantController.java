package com.gopesh.identity.tenant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Public tenant discovery endpoints")
public class TenantController {

    private final TenantConfigRegistry registry;

    @GetMapping
    @Operation(summary = "List all registered tenant IDs")
    public ResponseEntity<Set<String>> listTenants() {
        return ResponseEntity.ok(registry.getAllConfigs().keySet());
    }

    @GetMapping("/{tenantId}")
    @Operation(summary = "Get public configuration for a specific tenant")
    public ResponseEntity<Map<String, Object>> getTenantInfo(@PathVariable String tenantId) {
        TenantConfigProperties.TenantConfig config = registry.getConfig(tenantId);
        Map<String, Object> publicInfo = Map.of(
                "tenantId", config.getTenantId(),
                "displayName", config.getDisplayName(),
                "enabled", config.isEnabled(),
                "passwordPolicy", Map.of(
                        "minLength", config.getPasswordPolicy().getMinLength(),
                        "requireUppercase", config.getPasswordPolicy().isRequireUppercase(),
                        "requireSpecialChar", config.getPasswordPolicy().isRequireSpecialChar()
                )
        );
        return ResponseEntity.ok(publicInfo);
    }
}
