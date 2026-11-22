package com.hotelbooking.domain.property;

import com.hotelbooking.domain.exception.ValidationException;

public enum RoomCategory {
    STANDARD,
    DELUXE,
    EXECUTIVE,
    SUITE,
    FAMILY,
    VILLA;

    public static RoomCategory fromString(String value) {
        if (value == null || value.isBlank()) {
            return STANDARD;
        }
        try {
            return RoomCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Unsupported room category: " + value);
        }
    }
}
