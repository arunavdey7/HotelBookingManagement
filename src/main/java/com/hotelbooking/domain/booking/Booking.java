package com.hotelbooking.domain.booking;

import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.exception.InvalidStateTransitionException;
import com.hotelbooking.domain.exception.ValidationException;
import com.hotelbooking.domain.pricing.PriceQuote;

import java.time.Instant;
import java.util.Objects;

public class Booking {

    private final String id;
    private final String propertyId;
    private final String roomTypeId;
    private final GuestDetails guest;
    private final DateRange dateRange;
    private final int numberOfGuests;
    private final int roomsBooked;
    private final PriceQuote priceQuote;
    private BookingStatus status;
    private String paymentId;
    private CancellationDetails cancellationDetails;
    private final Instant createdAt;
    private Instant updatedAt;

    public Booking(
            String id,
            String propertyId,
            String roomTypeId,
            GuestDetails guest,
            DateRange dateRange,
            int numberOfGuests,
            int roomsBooked,
            PriceQuote priceQuote,
            Instant createdAt
    ) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("Booking id cannot be blank");
        }
        if (propertyId == null || propertyId.isBlank()) {
            throw new ValidationException("Property id is required for booking");
        }
        if (roomTypeId == null || roomTypeId.isBlank()) {
            throw new ValidationException("RoomType id is required for booking");
        }
        if (numberOfGuests < 1) {
            throw new ValidationException("Number of guests must be at least 1");
        }
        if (roomsBooked < 1) {
            throw new ValidationException("Rooms booked must be at least 1");
        }

        this.id = id.trim();
        this.propertyId = propertyId.trim();
        this.roomTypeId = roomTypeId.trim();
        this.guest = Objects.requireNonNull(guest, "Guest details cannot be null");
        this.dateRange = Objects.requireNonNull(dateRange, "DateRange cannot be null");
        this.numberOfGuests = numberOfGuests;
        this.roomsBooked = roomsBooked;
        this.priceQuote = Objects.requireNonNull(priceQuote, "PriceQuote cannot be null");
        this.status = BookingStatus.PENDING_PAYMENT;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = createdAt;
    }

    public synchronized void confirmPayment(String paymentId, Instant timestamp) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new ValidationException("paymentId is required to confirm booking");
        }
        transitionTo(BookingStatus.CONFIRMED, timestamp);
        this.paymentId = paymentId.trim();
    }

    public synchronized void markPaymentFailed(String paymentId, Instant timestamp) {
        transitionTo(BookingStatus.PAYMENT_FAILED, timestamp);
        this.paymentId = paymentId == null ? null : paymentId.trim();
    }

    public synchronized void cancel(CancellationDetails details) {
        Objects.requireNonNull(details, "CancellationDetails cannot be null");
        transitionTo(BookingStatus.CANCELLED, details.cancelledAt());
        this.cancellationDetails = details;
    }

    public synchronized void complete(Instant timestamp) {
        transitionTo(BookingStatus.COMPLETED, timestamp);
    }

    private void transitionTo(BookingStatus target, Instant timestamp) {
        if (!this.status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(
                    "Invalid booking state transition from " + this.status + " to " + target
                            + " for booking " + this.id
            );
        }
        this.status = target;
        this.updatedAt = Objects.requireNonNull(timestamp, "timestamp cannot be null");
    }

    public synchronized boolean isHoldingInventory() {
        return status.holdsInventory();
    }

    public String getId() {
        return id;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public String getRoomTypeId() {
        return roomTypeId;
    }

    public GuestDetails getGuest() {
        return guest;
    }

    public DateRange getDateRange() {
        return dateRange;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public int getRoomsBooked() {
        return roomsBooked;
    }

    public PriceQuote getPriceQuote() {
        return priceQuote;
    }

    public synchronized BookingStatus getStatus() {
        return status;
    }

    public synchronized String getPaymentId() {
        return paymentId;
    }

    public synchronized CancellationDetails getCancellationDetails() {
        return cancellationDetails;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public synchronized Instant getUpdatedAt() {
        return updatedAt;
    }
}
