package com.hotelbooking.application.search.filter;

import com.hotelbooking.domain.common.Location;
import com.hotelbooking.domain.search.PropertySearchContext;
import com.hotelbooking.domain.search.PropertySearchCriteria;
import com.hotelbooking.domain.search.PropertySearchFilter;
import org.springframework.stereotype.Component;

@Component
public class LocationSearchFilter implements PropertySearchFilter {

    @Override
    public int order() {
        return 10;
    }

    @Override
    public boolean matches(PropertySearchContext context, PropertySearchCriteria criteria) {
        Location location = context.getProperty().getLocation();
        if (criteria.city() != null && !criteria.city().isBlank()
                && !location.matchesCity(criteria.city())) {
            return false;
        }
        if (criteria.locality() != null && !criteria.locality().isBlank()
                && !location.matchesLocality(criteria.locality())) {
            return false;
        }
        return true;
    }
}
