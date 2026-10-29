package com.gopesh.identity.exception;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String tenantId) {
        super("Tenant not found or disabled: " + tenantId);
    }
}
