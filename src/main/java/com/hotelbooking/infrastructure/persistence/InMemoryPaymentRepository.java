package com.hotelbooking.infrastructure.persistence;

import com.hotelbooking.domain.payment.Payment;
import com.hotelbooking.domain.payment.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<String, Payment> store = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        Objects.requireNonNull(payment, "payment cannot be null");
        store.put(payment.getId(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findById(String paymentId) {
        if (paymentId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(paymentId.trim()));
    }

    @Override
    public List<Payment> findByBookingId(String bookingId) {
        if (bookingId == null) {
            return List.of();
        }
        String normalized = bookingId.trim();
        return store.values().stream()
                .filter(payment -> payment.getBookingId().equals(normalized))
                .toList();
    }
}
