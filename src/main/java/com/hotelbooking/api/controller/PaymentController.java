package com.hotelbooking.api.controller;

import com.hotelbooking.api.dto.PaymentDtos;
import com.hotelbooking.api.mapper.ApiMapper;
import com.hotelbooking.api.mapper.PaymentInstrumentParserRegistry;
import com.hotelbooking.application.port.BookingUseCase;
import com.hotelbooking.application.port.PaymentUseCase;
import com.hotelbooking.domain.booking.Booking;
import com.hotelbooking.domain.payment.PaymentInstrumentDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/payments")
@Tag(name = "Payments", description = "Process idempotent payments via pluggable methods (CARD, UPI, WALLET)")
public class PaymentController {

    private final PaymentUseCase paymentUseCase;
    private final BookingUseCase bookingUseCase;
    private final PaymentInstrumentParserRegistry instrumentParserRegistry;

    public PaymentController(
            PaymentUseCase paymentUseCase,
            BookingUseCase bookingUseCase,
            PaymentInstrumentParserRegistry instrumentParserRegistry
    ) {
        this.paymentUseCase = paymentUseCase;
        this.bookingUseCase = bookingUseCase;
        this.instrumentParserRegistry = instrumentParserRegistry;
    }

    @PostMapping
    @Operation(summary = "Pay for a booking using CARD, UPI, or WALLET with optional Idempotency-Key")
    public PaymentDtos.PaymentResponse payForBooking(
            @PathVariable String bookingId,
            @RequestHeader(value = "Idempotency-Key", required = false) String headerIdempotencyKey,
            @Valid @RequestBody PaymentDtos.ProcessPaymentRequest request
    ) {
        PaymentInstrumentDetails instrument = instrumentParserRegistry.parse(
                request.method(),
                request.details()
        );
        String effectiveIdempotencyKey = (headerIdempotencyKey != null && !headerIdempotencyKey.isBlank())
                ? headerIdempotencyKey
                : request.idempotencyKey();

        PaymentUseCase.PaymentOutcome outcome = paymentUseCase.processPayment(
                new PaymentUseCase.ProcessPaymentCommand(bookingId, instrument, effectiveIdempotencyKey)
        );
        return ApiMapper.toPaymentResponse(outcome);
    }

    @GetMapping
    @Operation(summary = "List payment transactions associated with a booking")
    public List<PaymentDtos.PaymentResponse> getBookingPayments(@PathVariable String bookingId) {
        Booking booking = bookingUseCase.getBooking(bookingId);
        return paymentUseCase.getPaymentsForBooking(bookingId).stream()
                .map(payment -> ApiMapper.toPaymentResponse(payment, booking.getStatus().name(), false))
                .toList();
    }
}
