package com.hotelbooking.application.cancellation;

import com.hotelbooking.domain.booking.Booking;
import com.hotelbooking.domain.cancellation.CancellationPolicy;
import com.hotelbooking.domain.cancellation.RefundDecision;
import com.hotelbooking.domain.common.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;

@Component
public class FlexibleCancellationPolicy implements CancellationPolicy {

    public static final String CODE = "FLEXIBLE";

    private final int checkInHour;

    public FlexibleCancellationPolicy(CancellationProperties properties) {
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

        if (cancelledAt.isBefore(checkInInstant)) {
            return new RefundDecision(
                    paidTotal,
                    new BigDecimal("100.00"),
                    CODE,
                    "100% refund: cancelled before scheduled check-in"
            );
        }
        return new RefundDecision(
                Money.zero(paidTotal.currency()),
                BigDecimal.ZERO.setScale(2),
                CODE,
                "0% refund: cancelled after scheduled check-in time"
        );
    }
}
