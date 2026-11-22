package com.hotelbooking.application.service;

import com.hotelbooking.application.port.PaymentUseCase;
import com.hotelbooking.application.support.KeyedLockManager;
import com.hotelbooking.domain.booking.Booking;
import com.hotelbooking.domain.booking.BookingRepository;
import com.hotelbooking.domain.booking.BookingStatus;
import com.hotelbooking.domain.exception.IdempotencyConflictException;
import com.hotelbooking.domain.exception.InvalidStateTransitionException;
import com.hotelbooking.domain.exception.ResourceNotFoundException;
import com.hotelbooking.domain.exception.ValidationException;
import com.hotelbooking.domain.inventory.InventoryManager;
import com.hotelbooking.domain.payment.GatewayChargeResult;
import com.hotelbooking.domain.payment.IdempotencyRecord;
import com.hotelbooking.domain.payment.Payment;
import com.hotelbooking.domain.payment.PaymentProcessor;
import com.hotelbooking.domain.payment.PaymentRepository;
import com.hotelbooking.infrastructure.payment.PaymentProcessorRegistry;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService implements PaymentUseCase {

    private static final String BOOKING_LOCK_SCOPE = "BOOKING";
    private static final String IDEMPOTENCY_LOCK_SCOPE = "IDEMPOTENCY";

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final IdempotencyRecord.Repository idempotencyRepository;
    private final PaymentProcessorRegistry processorRegistry;
    private final InventoryManager inventoryManager;
    private final KeyedLockManager lockManager;
    private final Clock clock;

    public PaymentService(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            IdempotencyRecord.Repository idempotencyRepository,
            PaymentProcessorRegistry processorRegistry,
            InventoryManager inventoryManager,
            KeyedLockManager lockManager,
            Clock clock
    ) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.processorRegistry = processorRegistry;
        this.inventoryManager = inventoryManager;
        this.lockManager = lockManager;
        this.clock = clock;
    }

    @Override
    public PaymentOutcome processPayment(ProcessPaymentCommand command) {
        Objects.requireNonNull(command, "ProcessPaymentCommand cannot be null");
        if (command.bookingId() == null || command.bookingId().isBlank()) {
            throw new ValidationException("bookingId is required for payment");
        }
        if (command.instrumentDetails() == null) {
            throw new ValidationException("Payment instrument details are required");
        }

        String normalizedIdempotencyKey = normalizeKey(command.idempotencyKey());
        if (normalizedIdempotencyKey != null) {
            return lockManager.executeWithLock(
                    IDEMPOTENCY_LOCK_SCOPE,
                    normalizedIdempotencyKey,
                    () -> executeIdempotentPayment(command, normalizedIdempotencyKey)
            );
        }

        return executeChargeForBooking(command, null);
    }

    @Override
    public List<Payment> getPaymentsForBooking(String bookingId) {
        bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        return paymentRepository.findByBookingId(bookingId);
    }

    private PaymentOutcome executeIdempotentPayment(ProcessPaymentCommand command, String idempotencyKey) {
        String fingerprint = buildFingerprint(command);
        Optional<IdempotencyRecord> existingRecord = idempotencyRepository.findByKey(idempotencyKey);

        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();
            if (!record.requestFingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException(
                        "Idempotency key '" + idempotencyKey
                                + "' was already used with different payment parameters"
                );
            }
            Payment originalPayment = paymentRepository.findById(record.paymentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment", record.paymentId()));
            Booking currentBooking = bookingRepository.findById(originalPayment.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking", originalPayment.getBookingId()));
            return new PaymentOutcome(originalPayment, currentBooking, true);
        }

        PaymentOutcome outcome = executeChargeForBooking(command, idempotencyKey);
        idempotencyRepository.save(new IdempotencyRecord(
                idempotencyKey,
                fingerprint,
                outcome.payment().getId(),
                Instant.now(clock)
        ));
        return outcome;
    }

    private PaymentOutcome executeChargeForBooking(ProcessPaymentCommand command, String idempotencyKey) {
        Booking initialLookup = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", command.bookingId()));

        return lockManager.executeWithLock(BOOKING_LOCK_SCOPE, initialLookup.getId(), () -> {
            Booking booking = bookingRepository.findById(initialLookup.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking", initialLookup.getId()));

            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
                throw new InvalidStateTransitionException(
                        "Booking " + booking.getId() + " cannot accept payment in state " + booking.getStatus()
                );
            }

            PaymentProcessor processor = processorRegistry.getProcessor(
                    command.instrumentDetails().methodType()
            );

            Instant now = Instant.now(clock);
            Payment payment = new Payment(
                    "pay-" + UUID.randomUUID().toString().substring(0, 8),
                    booking.getId(),
                    booking.getPriceQuote().totalAmount(),
                    processor.supportedMethod(),
                    idempotencyKey,
                    now
            );

            GatewayChargeResult chargeResult = processor.charge(
                    booking.getId(),
                    payment.getAmount(),
                    command.instrumentDetails()
            );

            Instant completedAt = Instant.now(clock);
            if (chargeResult.success()) {
                payment.markSucceeded(chargeResult.transactionReference(), completedAt);
                booking.confirmPayment(payment.getId(), completedAt);
            } else {
                payment.markFailed(chargeResult.failureReason(), completedAt);
                booking.markPaymentFailed(payment.getId(), completedAt);
                inventoryManager.release(
                        booking.getPropertyId(),
                        booking.getRoomTypeId(),
                        booking.getDateRange(),
                        booking.getRoomsBooked()
                );
            }

            paymentRepository.save(payment);
            bookingRepository.save(booking);
            return new PaymentOutcome(payment, booking, false);
        });
    }

    private static String buildFingerprint(ProcessPaymentCommand command) {
        return command.bookingId().trim() + "|" + command.instrumentDetails().fingerprintSummary();
    }

    private static String normalizeKey(String key) {
        return (key == null || key.isBlank()) ? null : key.trim();
    }
}
