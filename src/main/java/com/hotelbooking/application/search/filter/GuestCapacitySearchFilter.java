package com.hotelbooking.application.search.filter;

import com.hotelbooking.domain.search.PropertySearchContext;
import com.hotelbooking.domain.search.PropertySearchCriteria;
import com.hotelbooking.domain.search.PropertySearchFilter;
import org.springframework.stereotype.Component;

@Component
public class GuestCapacitySearchFilter implements PropertySearchFilter {

    @Override
    public int order() {
        return 50;
    }

    @Override
    public boolean matches(PropertySearchContext context, PropertySearchCriteria criteria) {
        if (criteria.guests() == null) {
            return true;
        }
        int requestedGuests = criteria.guests();
        context.retainRoomsMatching(roomQuote -> roomQuote.roomType().canAccommodate(requestedGuests));
        return context.hasMatchingRooms();
    }
}
