package com.hotelbooking.domain.pricing;

import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.exception.ValidationException;

import java.util.List;

public record PriceQuote(
        Money baseAmount,
        List<PriceAdjustment> adjustments,
        Money totalAmount,
        Money averageNightlyRate,
        int nights
) {

    public PriceQuote {
        if (baseAmount == null || totalAmount == null || averageNightlyRate == null) {
            throw new ValidationException("PriceQuote monetary amounts cannot be null");
        }
        if (nights < 1) {
            throw new ValidationException("PriceQuote nights must be at least 1");
        }
        if (totalAmount.isNegative()) {
            throw new ValidationException("PriceQuote totalAmount cannot be negative");
        }
        adjustments = adjustments == null ? List.of() : List.copyOf(adjustments);
    }
}
