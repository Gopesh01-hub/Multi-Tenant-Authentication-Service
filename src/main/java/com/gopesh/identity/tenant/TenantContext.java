package com.gopesh.identity.tenant;

/**
 * Thread-local holder for the current tenant. Set by {@link TenantInterceptor}
 * on every inbound request and cleared after the response completes.
 *
 * <p>The tenant ID propagates automatically into JPA queries, audit logs,
 * and cache key prefixes so that data isolation requires zero caller effort.</p>
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
