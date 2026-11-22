package com.hotelbooking.domain.property;

import com.hotelbooking.domain.exception.ValidationException;

public enum PropertyType {
    HOTEL,
    RESORT,
    BOUTIQUE,
    HOMESTAY,
    APARTMENT,
    VILLA;

    public static PropertyType fromString(String value) {
        if (value == null || value.isBlank()) {
            return HOTEL;
        }
        try {
            return PropertyType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Unsupported property type: " + value);
        }
    }
}
