package com.hotelbooking.application.pricing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "booking.pricing")
public record PricingProperties(
        String mode,
        BigDecimal weekendMultiplier,
        Integer longStayMinNights,
        BigDecimal longStayDiscountRate
) {

    public PricingProperties {
        mode = (mode == null || mode.isBlank()) ? "dynamic" : mode.trim().toLowerCase();
        weekendMultiplier = weekendMultiplier == null ? new BigDecimal("1.20") : weekendMultiplier;
        longStayMinNights = longStayMinNights == null ? 7 : longStayMinNights;
        longStayDiscountRate = longStayDiscountRate == null ? new BigDecimal("0.10") : longStayDiscountRate;
    }
}
