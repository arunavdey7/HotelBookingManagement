package com.hotelbooking.domain.common;

import com.hotelbooking.domain.exception.ValidationException;

public record Location(
        String addressLine,
        String locality,
        String city,
        String state,
        String country,
        String postalCode
) {

    public Location {
        if (city == null || city.isBlank()) {
            throw new ValidationException("City is required for property location");
        }
        if (locality == null || locality.isBlank()) {
            throw new ValidationException("Locality is required for property location");
        }
        if (country == null || country.isBlank()) {
            country = "India";
        } else {
            country = country.trim();
        }
        addressLine = addressLine == null ? "" : addressLine.trim();
        locality = locality.trim();
        city = city.trim();
        state = state == null ? "" : state.trim();
        postalCode = postalCode == null ? "" : postalCode.trim();
    }

    public boolean matchesCity(String targetCity) {
        return targetCity != null && city.equalsIgnoreCase(targetCity.trim());
    }

    public boolean matchesLocality(String targetLocality) {
        return targetLocality != null && locality.equalsIgnoreCase(targetLocality.trim());
    }
}
