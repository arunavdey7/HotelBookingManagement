package com.hotelbooking.application.pricing;

import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.pricing.PriceAdjustment;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.RoomType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Optional;

@Component
public class WeekendSurgePricingRule implements PricingRule {

    private final BigDecimal surgeDelta;

    public WeekendSurgePricingRule(PricingProperties properties) {
        this.surgeDelta = properties.weekendMultiplier().subtract(BigDecimal.ONE);
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Optional<PriceAdjustment> evaluate(
            Property property,
            RoomType roomType,
            DateRange stayPeriod,
            int guests,
            Money runningTotal
    ) {
        if (surgeDelta.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        long weekendNights = stayPeriod.stayNights().stream()
                .filter(date -> date.getDayOfWeek() == DayOfWeek.FRIDAY
                        || date.getDayOfWeek() == DayOfWeek.SATURDAY)
                .count();
        if (weekendNights == 0) {
            return Optional.empty();
        }
        Money perNightSurge = roomType.basePricePerNight().multiply(surgeDelta);
        Money totalSurge = perNightSurge.multiply(Math.toIntExact(weekendNights));
        if (totalSurge.isZero()) {
            return Optional.empty();
        }
        return Optional.of(new PriceAdjustment(
                "Weekend surge (" + weekendNights + " Fri/Sat night(s))",
                totalSurge
        ));
    }
}
