package com.hotelbooking.domain.booking;

import com.hotelbooking.domain.common.Money;

import java.time.Instant;
import java.util.Objects;

public record CancellationDetails(
        Instant cancelledAt,
        String reason,
        String policyApplied,
        Money refundAmount,
        String refundReference
) {

    public CancellationDetails {
        Objects.requireNonNull(cancelledAt, "cancelledAt cannot be null");
        Objects.requireNonNull(policyApplied, "policyApplied cannot be null");
        Objects.requireNonNull(refundAmount, "refundAmount cannot be null");
        reason = reason == null ? "Guest requested cancellation" : reason.trim();
    }
}
