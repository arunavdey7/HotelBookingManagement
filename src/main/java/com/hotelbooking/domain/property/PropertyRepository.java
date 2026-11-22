package com.hotelbooking.domain.property;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository {

    Property save(Property property);

    Optional<Property> findById(String propertyId);

    List<Property> findByOwnerId(String ownerId);

    List<Property> findAllActive();
}
