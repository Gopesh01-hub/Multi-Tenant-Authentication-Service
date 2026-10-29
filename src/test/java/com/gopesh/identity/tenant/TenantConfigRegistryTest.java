package com.gopesh.identity.tenant;

import com.gopesh.identity.exception.TenantNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class TenantConfigRegistryTest {

    private TenantConfigRegistry registry;

    @BeforeEach
    void setUp() {
        TenantConfigProperties properties = new TenantConfigProperties();

        TenantConfigProperties.TenantConfig skyConfig = new TenantConfigProperties.TenantConfig();
        skyConfig.setTenantId("sky-uk");
        skyConfig.setDisplayName("Sky UK");
        skyConfig.setEnabled(true);
        skyConfig.setMaxSessions(5);

        TenantConfigProperties.TenantConfig disabledConfig = new TenantConfigProperties.TenantConfig();
        disabledConfig.setTenantId("disabled-tenant");
        disabledConfig.setDisplayName("Disabled");
        disabledConfig.setEnabled(false);

        properties.setConfigs(List.of(skyConfig, disabledConfig));

        registry = new TenantConfigRegistry(properties);
        registry.init();
    }

    @Test
    @DisplayName("Should return config for a valid, enabled tenant")
    void shouldReturnConfigForValidTenant() {
        TenantConfigProperties.TenantConfig config = registry.getConfig("sky-uk");

        assertThat(config.getDisplayName()).isEqualTo("Sky UK");
        assertThat(config.getMaxSessions()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should throw TenantNotFoundException for unknown tenant")
    void shouldThrowForUnknownTenant() {
        assertThatThrownBy(() -> registry.getConfig("nonexistent"))
                .isInstanceOf(TenantNotFoundException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    @DisplayName("Should throw TenantNotFoundException for disabled tenant")
    void shouldThrowForDisabledTenant() {
        assertThatThrownBy(() -> registry.getConfig("disabled-tenant"))
                .isInstanceOf(TenantNotFoundException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    @DisplayName("Should correctly report tenant existence")
    void shouldCheckExistence() {
        assertThat(registry.exists("sky-uk")).isTrue();
        assertThat(registry.exists("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("Should return all configs as an immutable map")
    void shouldReturnAllConfigs() {
        assertThat(registry.getAllConfigs()).hasSize(2);
        assertThatThrownBy(() -> registry.getAllConfigs().put("hack", null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
