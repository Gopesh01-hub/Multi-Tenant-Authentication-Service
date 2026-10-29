package com.gopesh.identity.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(resourceType + " not found with id: " + identifier);
    }
}
