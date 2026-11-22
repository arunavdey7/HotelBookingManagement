package com.hotelbooking.application.port;

import com.hotelbooking.domain.booking.Booking;
import com.hotelbooking.domain.payment.Payment;
import com.hotelbooking.domain.payment.PaymentInstrumentDetails;

import java.util.List;

public interface PaymentUseCase {

    PaymentOutcome processPayment(ProcessPaymentCommand command);

    List<Payment> getPaymentsForBooking(String bookingId);

    record ProcessPaymentCommand(
            String bookingId,
            PaymentInstrumentDetails instrumentDetails,
            String idempotencyKey
    ) {
    }

    record PaymentOutcome(
            Payment payment,
            Booking booking,
            boolean idempotentReplay
    ) {
    }
}
