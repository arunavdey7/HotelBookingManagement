package com.hotelbooking.domain.payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(String paymentId);

    List<Payment> findByBookingId(String bookingId);
}
