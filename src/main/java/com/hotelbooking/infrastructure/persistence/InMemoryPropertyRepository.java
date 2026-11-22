package com.hotelbooking.infrastructure.persistence;

import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.PropertyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPropertyRepository implements PropertyRepository {

    private final Map<String, Property> store = new ConcurrentHashMap<>();

    @Override
    public Property save(Property property) {
        Objects.requireNonNull(property, "property cannot be null");
        store.put(property.getId(), property);
        return property;
    }

    @Override
    public Optional<Property> findById(String propertyId) {
        if (propertyId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(propertyId.trim()));
    }

    @Override
    public List<Property> findByOwnerId(String ownerId) {
        if (ownerId == null) {
            return List.of();
        }
        String normalized = ownerId.trim();
        return store.values().stream()
                .filter(property -> property.getOwnerId().equals(normalized))
                .toList();
    }

    @Override
    public List<Property> findAllActive() {
        return store.values().stream()
                .filter(Property::isActive)
                .toList();
    }
}
