package com.hotelbooking.domain.booking;

import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(String bookingId);

    List<Booking> findByPropertyId(String propertyId);

    List<Booking> findAll();
}
