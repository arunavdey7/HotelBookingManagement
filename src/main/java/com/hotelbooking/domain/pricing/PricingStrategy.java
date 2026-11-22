package com.hotelbooking.domain.pricing;

import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.RoomType;

public interface PricingStrategy {

    PriceQuote quote(Property property, RoomType roomType, DateRange stayPeriod, int guests);
}
