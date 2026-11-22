package com.hotelbooking.domain.search;

import com.hotelbooking.domain.property.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Mutable evaluation context passed through the chain of PropertySearchFilters.
 * Allows property-level filters to accept/reject a property, and room-level filters
 * (capacity, date availability, price range) to narrow down the matching room options
 * so a property is returned if and only if at least one room option satisfies all criteria.
 */
public class PropertySearchContext {

    private final Property property;
    private List<AvailableRoomQuote> candidateRooms;

    public PropertySearchContext(Property property, List<AvailableRoomQuote> initialRooms) {
        this.property = Objects.requireNonNull(property, "property cannot be null");
        this.candidateRooms = new ArrayList<>(Objects.requireNonNull(initialRooms, "initialRooms cannot be null"));
    }

    public Property getProperty() {
        return property;
    }

    public List<AvailableRoomQuote> getCandidateRooms() {
        return Collections.unmodifiableList(candidateRooms);
    }

    public void retainRoomsMatching(Predicate<AvailableRoomQuote> predicate) {
        this.candidateRooms = this.candidateRooms.stream()
                .filter(predicate)
                .toList();
    }

    public boolean hasMatchingRooms() {
        return !candidateRooms.isEmpty();
    }
}
