package com.hotelbooking.domain.property;

import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.exception.ValidationException;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record RoomType(
        String id,
        String name,
        RoomCategory category,
        int maxOccupancy,
        int totalInventory,
        Money basePricePerNight,
        Set<Amenity> amenities
) {

    public RoomType {
        if (id == null || id.isBlank()) {
            throw new ValidationException("RoomType id cannot be blank");
        }
        if (name == null || name.isBlank()) {
            throw new ValidationException("RoomType name is required");
        }
        if (category == null) {
            category = RoomCategory.STANDARD;
        }
        if (maxOccupancy < 1) {
            throw new ValidationException("RoomType maxOccupancy must be at least 1");
        }
        if (totalInventory < 1) {
            throw new ValidationException("RoomType totalInventory must be at least 1");
        }
        if (basePricePerNight == null || !basePricePerNight.isPositive()) {
            throw new ValidationException("RoomType basePricePerNight must be strictly positive");
        }
        id = id.trim();
        name = name.trim();
        amenities = amenities == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(amenities));
    }

    public boolean canAccommodate(int guests) {
        return guests >= 1 && guests <= maxOccupancy;
    }
}
