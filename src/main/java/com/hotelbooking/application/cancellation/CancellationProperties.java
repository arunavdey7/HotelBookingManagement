package com.hotelbooking.application.cancellation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "booking.cancellation")
public record CancellationProperties(
        String defaultPolicy,
        Integer checkInHour
) {

    public CancellationProperties {
        defaultPolicy = (defaultPolicy == null || defaultPolicy.isBlank())
                ? "MODERATE_TIERED"
                : defaultPolicy.trim().toUpperCase();
        checkInHour = (checkInHour == null || checkInHour < 0 || checkInHour > 23) ? 14 : checkInHour;
    }
}
