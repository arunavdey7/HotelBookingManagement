package com.hotelbooking.domain.property;

import com.hotelbooking.domain.exception.ValidationException;

/**
 * Extensible normalized value object for property and room amenities.
 * Using a normalized value object instead of a closed enum allows new amenities
 * (e.g., EV_CHARGING, COWORKING_SPACE) to be onboarded and filtered without code changes.
 */
public record Amenity(String code) {

    public static final Amenity WIFI = Amenity.of("WIFI");
    public static final Amenity POOL = Amenity.of("POOL");
    public static final Amenity PARKING = Amenity.of("PARKING");
    public static final Amenity GYM = Amenity.of("GYM");
    public static final Amenity SPA = Amenity.of("SPA");
    public static final Amenity BREAKFAST = Amenity.of("BREAKFAST");
    public static final Amenity AIR_CONDITIONING = Amenity.of("AIR_CONDITIONING");
    public static final Amenity PET_FRIENDLY = Amenity.of("PET_FRIENDLY");

    public Amenity {
        if (code == null || code.isBlank()) {
            throw new ValidationException("Amenity code cannot be blank");
        }
        code = code.trim().toUpperCase().replace(' ', '_').replace('-', '_');
    }

    public static Amenity of(String raw) {
        return new Amenity(raw);
    }
}
