package com.hotelbooking.domain.booking;

import com.hotelbooking.domain.exception.ValidationException;

public record GuestDetails(String fullName, String email, String phone) {

    public GuestDetails {
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("Guest full name is required");
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new ValidationException("A valid guest email is required");
        }
        fullName = fullName.trim();
        email = email.trim().toLowerCase();
        phone = phone == null ? "" : phone.trim();
    }
}
