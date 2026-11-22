package com.hotelbooking.domain.property;

import com.hotelbooking.domain.common.Location;
import com.hotelbooking.domain.exception.ResourceNotFoundException;
import com.hotelbooking.domain.exception.ValidationException;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class Property {

    private final String id;
    private final String ownerId;
    private final String name;
    private final String description;
    private final PropertyType propertyType;
    private final Location location;
    private final int starRating;
    private final Set<Amenity> amenities;
    private final Map<String, RoomType> roomTypesById;
    private final String cancellationPolicyCode;
    private final boolean active;
    private final Instant createdAt;

    public Property(
            String id,
            String ownerId,
            String name,
            String description,
            PropertyType propertyType,
            Location location,
            int starRating,
            Set<Amenity> amenities,
            List<RoomType> roomTypes,
            String cancellationPolicyCode,
            Instant createdAt
    ) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("Property id cannot be blank");
        }
        if (ownerId == null || ownerId.isBlank()) {
            throw new ValidationException("Property must belong to an owner account");
        }
        if (name == null || name.isBlank()) {
            throw new ValidationException("Property name is required");
        }
        if (location == null) {
            throw new ValidationException("Property location is required");
        }
        if (starRating < 1 || starRating > 5) {
            throw new ValidationException("Property star rating must be between 1 and 5");
        }
        if (roomTypes == null || roomTypes.isEmpty()) {
            throw new ValidationException("Property must have at least one room type");
        }

        this.id = id.trim();
        this.ownerId = ownerId.trim();
        this.name = name.trim();
        this.description = description == null ? "" : description.trim();
        this.propertyType = propertyType == null ? PropertyType.HOTEL : propertyType;
        this.location = location;
        this.starRating = starRating;
        this.amenities = amenities == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(amenities));

        Map<String, RoomType> roomMap = new LinkedHashMap<>();
        for (RoomType roomType : roomTypes) {
            Objects.requireNonNull(roomType, "RoomType cannot be null");
            if (roomMap.putIfAbsent(roomType.id(), roomType) != null) {
                throw new ValidationException("Duplicate roomType id in property: " + roomType.id());
            }
        }
        this.roomTypesById = roomMap;
        this.cancellationPolicyCode = (cancellationPolicyCode == null || cancellationPolicyCode.isBlank())
                ? "MODERATE_TIERED"
                : cancellationPolicyCode.trim().toUpperCase();
        this.active = true;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public synchronized void addRoomType(RoomType roomType) {
        Objects.requireNonNull(roomType, "RoomType cannot be null");
        if (roomTypesById.containsKey(roomType.id())) {
            throw new ValidationException("RoomType already exists on property: " + roomType.id());
        }
        roomTypesById.put(roomType.id(), roomType);
    }

    public synchronized Optional<RoomType> findRoomType(String roomTypeId) {
        if (roomTypeId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(roomTypesById.get(roomTypeId.trim()));
    }

    public synchronized RoomType getRoomTypeOrThrow(String roomTypeId) {
        return findRoomType(roomTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("RoomType", roomTypeId));
    }

    public synchronized Collection<RoomType> getRoomTypes() {
        return Collections.unmodifiableCollection(new LinkedHashSet<>(roomTypesById.values()));
    }

    public boolean supportsAmenities(Set<Amenity> requiredAmenities) {
        if (requiredAmenities == null || requiredAmenities.isEmpty()) {
            return true;
        }
        return amenities.containsAll(requiredAmenities);
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public Location getLocation() {
        return location;
    }

    public int getStarRating() {
        return starRating;
    }

    public Set<Amenity> getAmenities() {
        return amenities;
    }

    public String getCancellationPolicyCode() {
        return cancellationPolicyCode;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
