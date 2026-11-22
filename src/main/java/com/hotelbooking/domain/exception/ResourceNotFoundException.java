package com.hotelbooking.domain.exception;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " not found with id: " + identifier);
    }
}
