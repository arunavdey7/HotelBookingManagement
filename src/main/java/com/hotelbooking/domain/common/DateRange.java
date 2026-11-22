package com.hotelbooking.domain.common;

import com.hotelbooking.domain.exception.ValidationException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Half-open date interval [checkIn, checkOut) representing a hotel stay.
 * Checking out on date D does not occupy the night of date D, allowing another guest to check in on D.
 */
public record DateRange(LocalDate checkIn, LocalDate checkOut) {

    public DateRange {
        if (checkIn == null || checkOut == null) {
            throw new ValidationException("Both check-in and check-out dates are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new ValidationException("Check-out date must be strictly after check-in date");
        }
    }

    public int nights() {
        return Math.toIntExact(ChronoUnit.DAYS.between(checkIn, checkOut));
    }

    public List<LocalDate> stayNights() {
        return checkIn.datesUntil(checkOut).toList();
    }

    public boolean overlaps(DateRange other) {
        if (other == null) {
            return false;
        }
        return this.checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(this.checkOut);
    }
}
