package com.hotelbooking.domain.exception;

public class InsufficientInventoryException extends DomainException {

    public InsufficientInventoryException(String message) {
        super(message);
    }
}
