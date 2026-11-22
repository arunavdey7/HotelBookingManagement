package com.hotelbooking.application.search.filter;

import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.search.PropertySearchContext;
import com.hotelbooking.domain.search.PropertySearchCriteria;
import com.hotelbooking.domain.search.PropertySearchFilter;
import org.springframework.stereotype.Component;

@Component
public class PriceRangeSearchFilter implements PropertySearchFilter {

    @Override
    public int order() {
        return 60;
    }

    @Override
    public boolean matches(PropertySearchContext context, PropertySearchCriteria criteria) {
        Money min = criteria.minPricePerNight();
        Money max = criteria.maxPricePerNight();
        if (min == null && max == null) {
            return true;
        }
        context.retainRoomsMatching(roomQuote -> {
            Money effectiveNightly = roomQuote.priceQuote().averageNightlyRate();
            boolean aboveMin = min == null || effectiveNightly.isGreaterThanOrEqual(min);
            boolean belowMax = max == null || effectiveNightly.isLessThanOrEqual(max);
            return aboveMin && belowMax;
        });
        return context.hasMatchingRooms();
    }
}
