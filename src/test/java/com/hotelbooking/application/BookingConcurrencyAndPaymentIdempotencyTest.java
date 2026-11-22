package com.hotelbooking.application;

import com.hotelbooking.application.cancellation.CancellationPolicyRegistry;
import com.hotelbooking.application.cancellation.CancellationProperties;
import com.hotelbooking.application.cancellation.FlexibleCancellationPolicy;
import com.hotelbooking.application.cancellation.NonRefundableCancellationPolicy;
import com.hotelbooking.application.cancellation.TieredCancellationPolicy;
import com.hotelbooking.application.port.BookingUseCase;
import com.hotelbooking.application.port.PaymentUseCase;
import com.hotelbooking.application.port.PropertyOnboardingUseCase;
import com.hotelbooking.application.pricing.FlatRatePricingStrategy;
import com.hotelbooking.application.service.BookingService;
import com.hotelbooking.application.service.PaymentService;
import com.hotelbooking.application.service.PropertyOnboardingService;
import com.hotelbooking.application.support.KeyedLockManager;
import com.hotelbooking.domain.booking.Booking;
import com.hotelbooking.domain.booking.BookingStatus;
import com.hotelbooking.domain.booking.GuestDetails;
import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.common.Location;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.exception.IdempotencyConflictException;
import com.hotelbooking.domain.exception.InsufficientInventoryException;
import com.hotelbooking.domain.payment.CardPaymentDetails;
import com.hotelbooking.domain.payment.PaymentStatus;
import com.hotelbooking.domain.payment.UpiPaymentDetails;
import com.hotelbooking.domain.payment.WalletPaymentDetails;
import com.hotelbooking.domain.property.Amenity;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.PropertyType;
import com.hotelbooking.domain.property.RoomCategory;
import com.hotelbooking.domain.property.RoomType;
import com.hotelbooking.infrastructure.inventory.InMemoryInventoryManager;
import com.hotelbooking.infrastructure.payment.CardPaymentProcessor;
import com.hotelbooking.infrastructure.payment.PaymentProcessorRegistry;
import com.hotelbooking.infrastructure.payment.UpiPaymentProcessor;
import com.hotelbooking.infrastructure.payment.WalletPaymentProcessor;
import com.hotelbooking.infrastructure.persistence.InMemoryBookingRepository;
import com.hotelbooking.infrastructure.persistence.InMemoryIdempotencyRepository;
import com.hotelbooking.infrastructure.persistence.InMemoryOwnerRepository;
import com.hotelbooking.infrastructure.persistence.InMemoryPaymentRepository;
import com.hotelbooking.infrastructure.persistence.InMemoryPropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingConcurrencyAndPaymentIdempotencyTest {

    private PropertyOnboardingService onboardingService;
    private BookingService bookingService;
    private PaymentService paymentService;
    private InMemoryInventoryManager inventoryManager;
    private Property property;
    private RoomType threeRoomInventoryType;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneOffset.UTC);
        InMemoryOwnerRepository ownerRepository = new InMemoryOwnerRepository();
        InMemoryPropertyRepository propertyRepository = new InMemoryPropertyRepository();
        InMemoryBookingRepository bookingRepository = new InMemoryBookingRepository();
        InMemoryPaymentRepository paymentRepository = new InMemoryPaymentRepository();
        InMemoryIdempotencyRepository idempotencyRepository = new InMemoryIdempotencyRepository();
        inventoryManager = new InMemoryInventoryManager();
        KeyedLockManager lockManager = new KeyedLockManager();

        CancellationProperties cancelProps = new CancellationProperties("MODERATE_TIERED", 14);
        CancellationPolicyRegistry policyRegistry = new CancellationPolicyRegistry(
                List.of(
                        new FlexibleCancellationPolicy(cancelProps),
                        new TieredCancellationPolicy(cancelProps),
                        new NonRefundableCancellationPolicy()
                ),
                cancelProps
        );

        PaymentProcessorRegistry processorRegistry = new PaymentProcessorRegistry(List.of(
                new CardPaymentProcessor(),
                new UpiPaymentProcessor(),
                new WalletPaymentProcessor()
        ));

        onboardingService = new PropertyOnboardingService(
                ownerRepository,
                propertyRepository,
                inventoryManager,
                policyRegistry,
                fixedClock
        );

        bookingService = new BookingService(
                bookingRepository,
                propertyRepository,
                paymentRepository,
                inventoryManager,
                new FlatRatePricingStrategy(),
                policyRegistry,
                processorRegistry,
                lockManager,
                fixedClock
        );

        paymentService = new PaymentService(
                bookingRepository,
                paymentRepository,
                idempotencyRepository,
                processorRegistry,
                inventoryManager,
                lockManager,
                fixedClock
        );

        property = onboardingService.onboardProperty(new PropertyOnboardingUseCase.OnboardPropertyCommand(
                null,
                null,
                "Harbour View Hotel",
                "Seafront hotel",
                PropertyType.HOTEL,
                new Location("1 Strand", "Colaba", "Mumbai", "MH", "India", "400005"),
                5,
                Set.of(Amenity.WIFI, Amenity.POOL),
                List.of(new PropertyOnboardingUseCase.RoomTypeInput(
                        "Sea View Room",
                        RoomCategory.DELUXE,
                        2,
                        3,
                        Money.inr(5000),
                        Set.of()
                )),
                "MODERATE_TIERED"
        ));
        threeRoomInventoryType = property.getRoomTypes().iterator().next();
    }

    @Test
    void concurrentBookingsOnSharedInventoryNeverDoubleBook() throws Exception {
        int threads = 20;
        DateRange targetDates = new DateRange(LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 24));
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                final int idx = i;
                tasks.add(() -> {
                    readyLatch.countDown();
                    startLatch.await();
                    try {
                        bookingService.createBooking(new BookingUseCase.CreateBookingCommand(
                                property.getId(),
                                threeRoomInventoryType.id(),
                                new GuestDetails("Guest " + idx, "guest" + idx + "@example.com", "9999999999"),
                                targetDates,
                                2,
                                1
                        ));
                        successCount.incrementAndGet();
                    } catch (InsufficientInventoryException ex) {
                        rejectedCount.incrementAndGet();
                    }
                    return null;
                });
            }

            List<Future<Void>> futures = new ArrayList<>();
            for (Callable<Void> task : tasks) {
                futures.add(pool.submit(task));
            }
            readyLatch.await();
            startLatch.countDown();
            for (Future<Void> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(successCount.get()).isEqualTo(3);
        assertThat(rejectedCount.get()).isEqualTo(17);
        assertThat(inventoryManager.getAvailableRooms(property.getId(), threeRoomInventoryType.id(), targetDates))
                .isZero();
    }

    @Test
    void supportsMultiplePaymentMethodsAndReleasesInventoryWhenPaymentFails() {
        DateRange dates = new DateRange(LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 7));

        Booking cardBooking = createSingleBooking(dates, "a@test.com");
        Booking upiBooking = createSingleBooking(dates, "b@test.com");
        Booking walletFailBooking = createSingleBooking(dates, "c@test.com");

        assertThat(inventoryManager.getAvailableRooms(property.getId(), threeRoomInventoryType.id(), dates))
                .isZero();

        PaymentUseCase.PaymentOutcome cardOutcome = paymentService.processPayment(
                new PaymentUseCase.ProcessPaymentCommand(
                        cardBooking.getId(),
                        new CardPaymentDetails("4111222233334444", "Asha", "12/29", "123"),
                        "idem-card-1"
                )
        );
        assertThat(cardOutcome.booking().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(cardOutcome.payment().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        PaymentUseCase.PaymentOutcome upiOutcome = paymentService.processPayment(
                new PaymentUseCase.ProcessPaymentCommand(
                        upiBooking.getId(),
                        new UpiPaymentDetails("asha@okicici"),
                        "idem-upi-1"
                )
        );
        assertThat(upiOutcome.booking().getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        // Third booking uses failing wallet -> transitions to PAYMENT_FAILED and releases its held room!
        PaymentUseCase.PaymentOutcome failedWallet = paymentService.processPayment(
                new PaymentUseCase.ProcessPaymentCommand(
                        walletFailBooking.getId(),
                        new WalletPaymentDetails("PAYTM", "acct-insufficient-balance"),
                        "idem-wallet-fail"
                )
        );
        assertThat(failedWallet.booking().getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(failedWallet.payment().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(inventoryManager.getAvailableRooms(property.getId(), threeRoomInventoryType.id(), dates))
                .isEqualTo(1);
    }

    @Test
    void paymentIdempotencyPreventsDuplicateChargesUnderConcurrentRetriesAndDetectsConflicts() throws Exception {
        DateRange dates = new DateRange(LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 14));
        Booking booking = createSingleBooking(dates, "idem@test.com");
        UpiPaymentDetails upi = new UpiPaymentDetails("traveler@ybl");

        int threads = 8;
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<PaymentUseCase.PaymentOutcome> outcomes = new ArrayList<>();

        try {
            List<Future<PaymentUseCase.PaymentOutcome>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    startLatch.await();
                    return paymentService.processPayment(new PaymentUseCase.ProcessPaymentCommand(
                            booking.getId(),
                            upi,
                            "shared-idem-key-123"
                    ));
                }));
            }
            startLatch.countDown();
            for (Future<PaymentUseCase.PaymentOutcome> future : futures) {
                outcomes.add(future.get());
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(outcomes).extracting(o -> o.payment().getId()).containsOnly(outcomes.get(0).payment().getId());
        long freshCount = outcomes.stream().filter(o -> !o.idempotentReplay()).count();
        long replayCount = outcomes.stream().filter(PaymentUseCase.PaymentOutcome::idempotentReplay).count();
        assertThat(freshCount).isEqualTo(1);
        assertThat(replayCount).isEqualTo(threads - 1);
        assertThat(paymentService.getPaymentsForBooking(booking.getId())).hasSize(1);

        // Reusing the same idempotency key with a DIFFERENT instrument or booking throws IdempotencyConflictException
        assertThatThrownBy(() -> paymentService.processPayment(new PaymentUseCase.ProcessPaymentCommand(
                booking.getId(),
                new UpiPaymentDetails("different@ybl"),
                "shared-idem-key-123"
        ))).isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void cancellingConfirmedBookingAppliesRefundAndRestoresInventoryForRebooking() {
        // Check-in 2026-12-01 (> 48h after fixed clock 2026-11-22) -> 100% refund under MODERATE_TIERED
        DateRange dates = new DateRange(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3));
        Booking b1 = createSingleBooking(dates, "one@test.com");
        Booking b2 = createSingleBooking(dates, "two@test.com");
        Booking b3 = createSingleBooking(dates, "three@test.com");

        assertThat(inventoryManager.getAvailableRooms(property.getId(), threeRoomInventoryType.id(), dates))
                .isZero();

        PaymentUseCase.PaymentOutcome paid = paymentService.processPayment(
                new PaymentUseCase.ProcessPaymentCommand(
                        b1.getId(),
                        new WalletPaymentDetails("AMAZON_PAY", "wlt-9001"),
                        "idem-wlt-9001"
                )
        );
        assertThat(paid.booking().getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        Booking cancelled = bookingService.cancelBooking(
                new BookingUseCase.CancelBookingCommand(b1.getId(), "Flight rescheduled")
        );
        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelled.getCancellationDetails().refundAmount().amount()).isEqualByComparingTo("10000.00");
        assertThat(cancelled.getCancellationDetails().refundReference()).startsWith("WLT-RFD-");
        assertThat(paymentService.getPaymentsForBooking(b1.getId()).get(0).getStatus())
                .isEqualTo(PaymentStatus.REFUNDED);

        // Inventory was released -> a 4th guest can now book those exact dates!
        assertThat(inventoryManager.getAvailableRooms(property.getId(), threeRoomInventoryType.id(), dates))
                .isEqualTo(1);
        Booking b4 = createSingleBooking(dates, "four@test.com");
        assertThat(b4.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    private Booking createSingleBooking(DateRange dates, String email) {
        return bookingService.createBooking(new BookingUseCase.CreateBookingCommand(
                property.getId(),
                threeRoomInventoryType.id(),
                new GuestDetails("Test Guest", email, "9888888888"),
                dates,
                2,
                1
        ));
    }
}
