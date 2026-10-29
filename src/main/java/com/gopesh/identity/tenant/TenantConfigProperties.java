package com.gopesh.identity.tenant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "identity.tenant")
public class TenantConfigProperties {

    @NotBlank
    private String headerName = "X-Tenant-ID";

    @NotBlank
    private String defaultTenant = "default";

    @Valid
    private List<TenantConfig> configs = new ArrayList<>();

    @Data
    public static class TenantConfig {

        @NotBlank
        private String tenantId;

        @NotBlank
        private String displayName;

        private boolean enabled = true;

        private int maxSessions = 3;

        @Valid
        @NotNull
        private PasswordPolicy passwordPolicy = new PasswordPolicy();

        @Valid
        @NotNull
        private RateLimit rateLimit = new RateLimit();

        private List<String> allowedOrigins = new ArrayList<>();
    }

    @Data
    public static class PasswordPolicy {
        private int minLength = 8;
        private boolean requireUppercase = false;
        private boolean requireSpecialChar = false;
        private int maxAgeDays = 180;
    }

    @Data
    public static class RateLimit {
        private int requestsPerMinute = 200;
        private int burstSize = 20;
    }
}
