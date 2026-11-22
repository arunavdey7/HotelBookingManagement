package com.hotelbooking.application.search.filter;

import com.hotelbooking.domain.property.Amenity;
import com.hotelbooking.domain.search.PropertySearchContext;
import com.hotelbooking.domain.search.PropertySearchCriteria;
import com.hotelbooking.domain.search.PropertySearchFilter;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class AmenitiesSearchFilter implements PropertySearchFilter {

    @Override
    public int order() {
        return 40;
    }

    @Override
    public boolean matches(PropertySearchContext context, PropertySearchCriteria criteria) {
        Set<Amenity> required = criteria.requiredAmenities();
        if (required == null || required.isEmpty()) {
            return true;
        }
        Set<Amenity> propertyAmenities = context.getProperty().getAmenities();
        if (propertyAmenities.containsAll(required)) {
            return true;
        }
        context.retainRoomsMatching(roomQuote -> {
            Set<Amenity> combined = new HashSet<>(propertyAmenities);
            combined.addAll(roomQuote.roomType().amenities());
            return combined.containsAll(required);
        });
        return context.hasMatchingRooms();
    }
}
