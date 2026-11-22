package com.hotelbooking.application.pricing;

import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.pricing.PriceAdjustment;
import com.hotelbooking.domain.pricing.PriceQuote;
import com.hotelbooking.domain.pricing.PricingStrategy;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.RoomType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class DynamicPricingStrategy implements PricingStrategy {

    private final List<PricingRule> rules;

    public DynamicPricingStrategy(List<PricingRule> rules) {
        this.rules = rules == null
                ? List.of()
                : rules.stream().sorted(Comparator.comparingInt(PricingRule::order)).toList();
    }

    @Override
    public PriceQuote quote(Property property, RoomType roomType, DateRange stayPeriod, int guests) {
        int nights = stayPeriod.nights();
        Money baseAmount = roomType.basePricePerNight().multiply(nights);
        Money runningTotal = baseAmount;
        List<PriceAdjustment> adjustments = new ArrayList<>();

        for (PricingRule rule : rules) {
            Optional<PriceAdjustment> adjustment = rule.evaluate(property, roomType, stayPeriod, guests, runningTotal);
            if (adjustment.isPresent()) {
                PriceAdjustment item = adjustment.get();
                adjustments.add(item);
                runningTotal = runningTotal.add(item.amount());
            }
        }

        if (runningTotal.isNegative()) {
            runningTotal = Money.zero(baseAmount.currency());
        }
        Money averageNightly = runningTotal.divide(nights);
        return new PriceQuote(baseAmount, adjustments, runningTotal, averageNightly, nights);
    }
}
