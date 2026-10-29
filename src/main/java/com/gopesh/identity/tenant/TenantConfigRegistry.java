package com.gopesh.identity.tenant;

import com.gopesh.identity.exception.TenantNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration-driven registry of tenant settings.
 * Loads all tenant configs at startup into an in-memory map for O(1) lookups.
 * New tenants can be onboarded through YAML config alone — zero code changes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantConfigRegistry {

    private final TenantConfigProperties properties;
    private final Map<String, TenantConfigProperties.TenantConfig> registry = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        properties.getConfigs().forEach(config -> {
            registry.put(config.getTenantId(), config);
            log.info("Registered tenant: {} ({})", config.getTenantId(), config.getDisplayName());
        });
        log.info("Total tenants registered: {}", registry.size());
    }

    public TenantConfigProperties.TenantConfig getConfig(String tenantId) {
        TenantConfigProperties.TenantConfig config = registry.get(tenantId);
        if (config == null) {
            throw new TenantNotFoundException(tenantId);
        }
        if (!config.isEnabled()) {
            throw new TenantNotFoundException("Tenant '" + tenantId + "' is disabled");
        }
        return config;
    }

    public boolean exists(String tenantId) {
        return registry.containsKey(tenantId);
    }

    public Map<String, TenantConfigProperties.TenantConfig> getAllConfigs() {
        return Map.copyOf(registry);
    }
}
