package com.hotelbooking.application.cancellation;

import com.hotelbooking.domain.booking.Booking;
import com.hotelbooking.domain.cancellation.CancellationPolicy;
import com.hotelbooking.domain.cancellation.RefundDecision;
import com.hotelbooking.domain.common.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;

@Component
public class TieredCancellationPolicy implements CancellationPolicy {

    public static final String CODE = "MODERATE_TIERED";

    private final int checkInHour;

    public TieredCancellationPolicy(CancellationProperties properties) {
        this.checkInHour = properties.checkInHour();
    }

    @Override
    public String policyCode() {
        return CODE;
    }

    @Override
    public RefundDecision evaluateRefund(Booking booking, Instant cancelledAt) {
        Money paidTotal = booking.getPriceQuote().totalAmount();
        Instant checkInInstant = booking.getDateRange().checkIn()
                .atTime(LocalTime.of(checkInHour, 0))
                .toInstant(ZoneOffset.UTC);

        long hoursBeforeCheckIn = Duration.between(cancelledAt, checkInInstant).toHours();

        if (hoursBeforeCheckIn >= 48) {
            return new RefundDecision(
                    paidTotal,
                    new BigDecimal("100.00"),
                    CODE,
                    "100% refund: cancelled at least 48 hours prior to check-in"
            );
        }
        if (hoursBeforeCheckIn >= 24) {
            Money halfRefund = paidTotal.multiply(new BigDecimal("0.50"));
            return new RefundDecision(
                    halfRefund,
                    new BigDecimal("50.00"),
                    CODE,
                    "50% refund: cancelled between 24 and 48 hours prior to check-in"
            );
        }
        return new RefundDecision(
                Money.zero(paidTotal.currency()),
                BigDecimal.ZERO.setScale(2),
                CODE,
                "0% refund: cancelled less than 24 hours prior to check-in"
        );
    }
}
