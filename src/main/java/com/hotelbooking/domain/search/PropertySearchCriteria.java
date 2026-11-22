package com.hotelbooking.domain.search;

import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.exception.ValidationException;
import com.hotelbooking.domain.property.Amenity;
import com.hotelbooking.domain.property.PropertyType;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record PropertySearchCriteria(
        String city,
        String locality,
        DateRange dateRange,
        Integer guests,
        Money minPricePerNight,
        Money maxPricePerNight,
        Set<Amenity> requiredAmenities,
        Integer minStarRating,
        PropertyType propertyType
) {

    public PropertySearchCriteria {
        if (guests != null && guests < 1) {
            throw new ValidationException("Number of guests must be at least 1");
        }
        if (minStarRating != null && (minStarRating < 1 || minStarRating > 5)) {
            throw new ValidationException("Minimum star rating must be between 1 and 5");
        }
        if (minPricePerNight != null && minPricePerNight.isNegative()) {
            throw new ValidationException("Minimum price cannot be negative");
        }
        if (maxPricePerNight != null && maxPricePerNight.isNegative()) {
            throw new ValidationException("Maximum price cannot be negative");
        }
        if (minPricePerNight != null && maxPricePerNight != null
                && minPricePerNight.compareTo(maxPricePerNight) > 0) {
            throw new ValidationException("minPricePerNight cannot exceed maxPricePerNight");
        }
        requiredAmenities = requiredAmenities == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(requiredAmenities));
    }
}
