package com.hotelbooking.infrastructure.persistence;

import com.hotelbooking.domain.booking.Booking;
import com.hotelbooking.domain.booking.BookingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBookingRepository implements BookingRepository {

    private final Map<String, Booking> store = new ConcurrentHashMap<>();

    @Override
    public Booking save(Booking booking) {
        Objects.requireNonNull(booking, "booking cannot be null");
        store.put(booking.getId(), booking);
        return booking;
    }

    @Override
    public Optional<Booking> findById(String bookingId) {
        if (bookingId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(bookingId.trim()));
    }

    @Override
    public List<Booking> findByPropertyId(String propertyId) {
        if (propertyId == null) {
            return List.of();
        }
        String normalized = propertyId.trim();
        return store.values().stream()
                .filter(booking -> booking.getPropertyId().equals(normalized))
                .toList();
    }

    @Override
    public List<Booking> findAll() {
        return List.copyOf(store.values());
    }
}
