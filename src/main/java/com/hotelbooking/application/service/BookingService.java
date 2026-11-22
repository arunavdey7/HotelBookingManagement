package com.hotelbooking.application.service;

import com.hotelbooking.application.cancellation.CancellationPolicyRegistry;
import com.hotelbooking.application.port.BookingUseCase;
import com.hotelbooking.application.support.KeyedLockManager;
import com.hotelbooking.domain.booking.Booking;
import com.hotelbooking.domain.booking.BookingRepository;
import com.hotelbooking.domain.booking.BookingStatus;
import com.hotelbooking.domain.booking.CancellationDetails;
import com.hotelbooking.domain.cancellation.CancellationPolicy;
import com.hotelbooking.domain.cancellation.RefundDecision;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.exception.InvalidStateTransitionException;
import com.hotelbooking.domain.exception.PaymentProcessingException;
import com.hotelbooking.domain.exception.ResourceNotFoundException;
import com.hotelbooking.domain.exception.ValidationException;
import com.hotelbooking.domain.inventory.InventoryManager;
import com.hotelbooking.domain.payment.GatewayRefundResult;
import com.hotelbooking.domain.payment.Payment;
import com.hotelbooking.domain.payment.PaymentProcessor;
import com.hotelbooking.domain.payment.PaymentRepository;
import com.hotelbooking.domain.pricing.PriceAdjustment;
import com.hotelbooking.domain.pricing.PriceQuote;
import com.hotelbooking.domain.pricing.PricingStrategy;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.PropertyRepository;
import com.hotelbooking.domain.property.RoomType;
import com.hotelbooking.infrastructure.payment.PaymentProcessorRegistry;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class BookingService implements BookingUseCase {

    private static final String BOOKING_LOCK_SCOPE = "BOOKING";

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryManager inventoryManager;
    private final PricingStrategy pricingStrategy;
    private final CancellationPolicyRegistry cancellationPolicyRegistry;
    private final PaymentProcessorRegistry paymentProcessorRegistry;
    private final KeyedLockManager lockManager;
    private final Clock clock;

    public BookingService(
            BookingRepository bookingRepository,
            PropertyRepository propertyRepository,
            PaymentRepository paymentRepository,
            InventoryManager inventoryManager,
            PricingStrategy pricingStrategy,
            CancellationPolicyRegistry cancellationPolicyRegistry,
            PaymentProcessorRegistry paymentProcessorRegistry,
            KeyedLockManager lockManager,
            Clock clock
    ) {
        this.bookingRepository = bookingRepository;
        this.propertyRepository = propertyRepository;
        this.paymentRepository = paymentRepository;
        this.inventoryManager = inventoryManager;
        this.pricingStrategy = pricingStrategy;
        this.cancellationPolicyRegistry = cancellationPolicyRegistry;
        this.paymentProcessorRegistry = paymentProcessorRegistry;
        this.lockManager = lockManager;
        this.clock = clock;
    }

    @Override
    public Booking createBooking(CreateBookingCommand command) {
        Objects.requireNonNull(command, "CreateBookingCommand cannot be null");
        if (command.dateRange() == null) {
            throw new ValidationException("Date range is required to create a booking");
        }
        LocalDate today = LocalDate.now(clock);
        if (command.dateRange().checkIn().isBefore(today)) {
            throw new ValidationException("Check-in date cannot be in the past");
        }

        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property", command.propertyId()));
        RoomType roomType = property.getRoomTypeOrThrow(command.roomTypeId());

        int roomsBooked = command.roomsRequested() == null ? 1 : command.roomsRequested();
        if (roomsBooked < 1) {
            throw new ValidationException("roomsRequested must be at least 1");
        }
        int maxTotalGuests = roomType.maxOccupancy() * roomsBooked;
        if (command.numberOfGuests() < 1 || command.numberOfGuests() > maxTotalGuests) {
            throw new ValidationException(
                    "Guest count " + command.numberOfGuests() + " exceeds maximum occupancy of "
                            + maxTotalGuests + " for " + roomsBooked + " room(s) of type " + roomType.name()
            );
        }

        PriceQuote singleRoomQuote = pricingStrategy.quote(
                property,
                roomType,
                command.dateRange(),
                command.numberOfGuests()
        );
        PriceQuote totalQuote = scaleQuoteForRooms(singleRoomQuote, roomsBooked);

        inventoryManager.reserve(property.getId(), roomType.id(), command.dateRange(), roomsBooked);

        Booking booking = new Booking(
                "bkg-" + UUID.randomUUID().toString().substring(0, 8),
                property.getId(),
                roomType.id(),
                command.guest(),
                command.dateRange(),
                command.numberOfGuests(),
                roomsBooked,
                totalQuote,
                Instant.now(clock)
        );
        return bookingRepository.save(booking);
    }

    @Override
    public Booking getBooking(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }

    @Override
    public List<Booking> getBookingsForProperty(String propertyId) {
        propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));
        return bookingRepository.findByPropertyId(propertyId);
    }

    @Override
    public Booking cancelBooking(CancelBookingCommand command) {
        Objects.requireNonNull(command, "CancelBookingCommand cannot be null");
        Booking existing = getBooking(command.bookingId());

        return lockManager.executeWithLock(BOOKING_LOCK_SCOPE, existing.getId(), () -> {
            Booking booking = getBooking(existing.getId());
            BookingStatus statusBeforeCancel = booking.getStatus();

            if (!statusBeforeCancel.canTransitionTo(BookingStatus.CANCELLED)) {
                throw new InvalidStateTransitionException(
                        "Cannot cancel booking " + booking.getId() + " in state " + statusBeforeCancel
                );
            }

            Instant cancelledAt = Instant.now(clock);
            CancellationDetails cancellationDetails;

            if (statusBeforeCancel == BookingStatus.CONFIRMED) {
                cancellationDetails = processConfirmedCancellation(booking, command.reason(), cancelledAt);
            } else {
                Money zeroRefund = Money.zero(booking.getPriceQuote().totalAmount().currency());
                cancellationDetails = new CancellationDetails(
                        cancelledAt,
                        command.reason(),
                        "UNPAID_BOOKING_RELEASE",
                        zeroRefund,
                        null
                );
            }

            booking.cancel(cancellationDetails);
            bookingRepository.save(booking);

            inventoryManager.release(
                    booking.getPropertyId(),
                    booking.getRoomTypeId(),
                    booking.getDateRange(),
                    booking.getRoomsBooked()
            );

            return booking;
        });
    }

    private CancellationDetails processConfirmedCancellation(
            Booking booking,
            String reason,
            Instant cancelledAt
    ) {
        Property property = propertyRepository.findById(booking.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property", booking.getPropertyId()));
        CancellationPolicy policy = cancellationPolicyRegistry.resolve(property.getCancellationPolicyCode());
        RefundDecision decision = policy.evaluateRefund(booking, cancelledAt);

        String refundReference = null;
        if (decision.refundAmount().isPositive() && booking.getPaymentId() != null) {
            Payment payment = paymentRepository.findById(booking.getPaymentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment", booking.getPaymentId()));
            PaymentProcessor processor = paymentProcessorRegistry.getProcessor(payment.getMethodType());
            GatewayRefundResult refundResult = processor.refund(payment, decision.refundAmount());
            if (!refundResult.success()) {
                throw new PaymentProcessingException(
                        "Gateway refund failed: " + refundResult.failureReason()
                );
            }
            refundReference = refundResult.refundReference();
            payment.recordRefund(decision.refundAmount(), cancelledAt);
            paymentRepository.save(payment);
        }

        return new CancellationDetails(
                cancelledAt,
                reason,
                decision.policyCode() + " (" + decision.explanation() + ")",
                decision.refundAmount(),
                refundReference
        );
    }

    private PriceQuote scaleQuoteForRooms(PriceQuote singleRoomQuote, int roomsBooked) {
        if (roomsBooked == 1) {
            return singleRoomQuote;
        }
        List<PriceAdjustment> scaledAdjustments = singleRoomQuote.adjustments().stream()
                .map(adj -> new PriceAdjustment(adj.description(), adj.amount().multiply(roomsBooked)))
                .toList();
        return new PriceQuote(
                singleRoomQuote.baseAmount().multiply(roomsBooked),
                scaledAdjustments,
                singleRoomQuote.totalAmount().multiply(roomsBooked),
                singleRoomQuote.averageNightlyRate().multiply(roomsBooked),
                singleRoomQuote.nights()
        );
    }
}
