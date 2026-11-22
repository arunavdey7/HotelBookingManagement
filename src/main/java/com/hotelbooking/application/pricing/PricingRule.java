package com.hotelbooking.application.pricing;

import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.pricing.PriceAdjustment;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.RoomType;

import java.util.Optional;

public interface PricingRule {

    default int order() {
        return 100;
    }

    Optional<PriceAdjustment> evaluate(
            Property property,
            RoomType roomType,
            DateRange stayPeriod,
            int guests,
            Money runningTotal
    );
}
