package com.hotelbooking.domain.cancellation;

import com.hotelbooking.domain.booking.Booking;

import java.time.Instant;

/**
 * Pluggable cancellation and refund policy strategy.
 */
public interface CancellationPolicy {

    String policyCode();

    RefundDecision evaluateRefund(Booking booking, Instant cancelledAt);
}
