package com.hotelbooking.application.search.filter;

import com.hotelbooking.domain.search.PropertySearchContext;
import com.hotelbooking.domain.search.PropertySearchCriteria;
import com.hotelbooking.domain.search.PropertySearchFilter;
import org.springframework.stereotype.Component;

@Component
public class PropertyTypeSearchFilter implements PropertySearchFilter {

    @Override
    public int order() {
        return 20;
    }

    @Override
    public boolean matches(PropertySearchContext context, PropertySearchCriteria criteria) {
        if (criteria.propertyType() == null) {
            return true;
        }
        return context.getProperty().getPropertyType() == criteria.propertyType();
    }
}
