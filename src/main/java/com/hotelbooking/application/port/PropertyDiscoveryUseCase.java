package com.hotelbooking.application.port;

import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.search.AvailableRoomQuote;
import com.hotelbooking.domain.search.PropertySearchCriteria;

import java.util.List;

public interface PropertyDiscoveryUseCase {

    List<PropertySearchResult> searchProperties(PropertySearchCriteria criteria);

    record PropertySearchResult(
            Property property,
            List<AvailableRoomQuote> availableRooms,
            Money startingFromPerNight
    ) {
    }
}
