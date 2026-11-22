package com.hotelbooking.application.pricing;

import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.pricing.PriceQuote;
import com.hotelbooking.domain.pricing.PricingStrategy;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.RoomType;

import java.util.List;

public class FlatRatePricingStrategy implements PricingStrategy {

    @Override
    public PriceQuote quote(Property property, RoomType roomType, DateRange stayPeriod, int guests) {
        int nights = stayPeriod.nights();
        Money baseAmount = roomType.basePricePerNight().multiply(nights);
        return new PriceQuote(
                baseAmount,
                List.of(),
                baseAmount,
                roomType.basePricePerNight(),
                nights
        );
    }
}
