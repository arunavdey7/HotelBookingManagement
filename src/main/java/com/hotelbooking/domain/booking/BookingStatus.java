package com.hotelbooking.domain.booking;

import java.util.EnumSet;
import java.util.Set;

public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    PAYMENT_FAILED,
    CANCELLED,
    COMPLETED;

    public boolean canTransitionTo(BookingStatus target) {
        if (target == null) {
            return false;
        }
        return allowedTargets().contains(target);
    }

    public Set<BookingStatus> allowedTargets() {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(CONFIRMED, PAYMENT_FAILED, CANCELLED);
            case CONFIRMED -> EnumSet.of(CANCELLED, COMPLETED);
            case PAYMENT_FAILED, CANCELLED, COMPLETED -> EnumSet.noneOf(BookingStatus.class);
        };
    }

    public boolean holdsInventory() {
        return this == PENDING_PAYMENT || this == CONFIRMED;
    }
}
