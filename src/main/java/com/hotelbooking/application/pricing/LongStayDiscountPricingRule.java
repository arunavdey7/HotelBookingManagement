package com.hotelbooking.application.pricing;

import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.pricing.PriceAdjustment;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.RoomType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class LongStayDiscountPricingRule implements PricingRule {

    private final int minNights;
    private final BigDecimal discountRate;

    public LongStayDiscountPricingRule(PricingProperties properties) {
        this.minNights = properties.longStayMinNights();
        this.discountRate = properties.longStayDiscountRate();
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Optional<PriceAdjustment> evaluate(
            Property property,
            RoomType roomType,
            DateRange stayPeriod,
            int guests,
            Money runningTotal
    ) {
        if (stayPeriod.nights() < minNights || discountRate.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        Money discount = runningTotal.multiply(discountRate).negate();
        if (discount.isZero()) {
            return Optional.empty();
        }
        return Optional.of(new PriceAdjustment(
                "Long-stay discount (" + stayPeriod.nights() + " nights)",
                discount
        ));
    }
}
