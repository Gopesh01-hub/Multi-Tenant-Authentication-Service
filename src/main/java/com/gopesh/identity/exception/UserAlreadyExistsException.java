package com.gopesh.identity.exception;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String email, String tenantId) {
        super("User with email '" + email + "' already exists in tenant '" + tenantId + "'");
    }
}
