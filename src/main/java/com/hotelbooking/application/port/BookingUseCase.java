package com.hotelbooking.application.port;

import com.hotelbooking.domain.booking.Booking;
import com.hotelbooking.domain.booking.GuestDetails;
import com.hotelbooking.domain.common.DateRange;

import java.util.List;

public interface BookingUseCase {

    Booking createBooking(CreateBookingCommand command);

    Booking getBooking(String bookingId);

    List<Booking> getBookingsForProperty(String propertyId);

    Booking cancelBooking(CancelBookingCommand command);

    record CreateBookingCommand(
            String propertyId,
            String roomTypeId,
            GuestDetails guest,
            DateRange dateRange,
            int numberOfGuests,
            Integer roomsRequested
    ) {
    }

    record CancelBookingCommand(
            String bookingId,
            String reason
    ) {
    }
}
