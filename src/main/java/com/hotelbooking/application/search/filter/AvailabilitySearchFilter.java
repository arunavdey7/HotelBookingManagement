package com.hotelbooking.application.search.filter;

import com.hotelbooking.domain.search.PropertySearchContext;
import com.hotelbooking.domain.search.PropertySearchCriteria;
import com.hotelbooking.domain.search.PropertySearchFilter;
import org.springframework.stereotype.Component;

@Component
public class AvailabilitySearchFilter implements PropertySearchFilter {

    @Override
    public int order() {
        return 70;
    }

    @Override
    public boolean matches(PropertySearchContext context, PropertySearchCriteria criteria) {
        context.retainRoomsMatching(roomQuote -> roomQuote.availableRooms() >= 1);
        return context.hasMatchingRooms();
    }
}
