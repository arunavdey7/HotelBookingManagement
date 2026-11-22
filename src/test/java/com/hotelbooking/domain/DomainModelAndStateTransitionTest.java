package com.hotelbooking.domain;

import com.hotelbooking.domain.booking.Booking;
import com.hotelbooking.domain.booking.BookingStatus;
import com.hotelbooking.domain.booking.CancellationDetails;
import com.hotelbooking.domain.booking.GuestDetails;
import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.exception.InvalidStateTransitionException;
import com.hotelbooking.domain.exception.ValidationException;
import com.hotelbooking.domain.owner.OwnerAccount;
import com.hotelbooking.domain.pricing.PriceQuote;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainModelAndStateTransitionTest {

    @Test
    void dateRangeUsesHalfOpenIntervalAllowingSameDayTurnaround() {
        DateRange firstStay = new DateRange(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 12));
        DateRange adjacentStay = new DateRange(LocalDate.of(2027, 1, 12), LocalDate.of(2027, 1, 14));
        DateRange overlappingStay = new DateRange(LocalDate.of(2027, 1, 11), LocalDate.of(2027, 1, 13));

        assertThat(firstStay.nights()).isEqualTo(2);
        assertThat(firstStay.stayNights()).containsExactly(
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 11)
        );
        assertThat(firstStay.overlaps(adjacentStay)).isFalse();
        assertThat(firstStay.overlaps(overlappingStay)).isTrue();
    }

    @Test
    void dateRangeRejectsInvalidCheckOutDates() {
        LocalDate date = LocalDate.of(2027, 1, 10);
        assertThatThrownBy(() -> new DateRange(date, date))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("strictly after");
        assertThatThrownBy(() -> new DateRange(date, date.minusDays(1)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void moneyEnforcesCurrencyConsistencyAndTwoDecimalScale() {
        Money inr100 = Money.of(100.126, "INR");
        Money usd50 = Money.of(50.00, "USD");

        assertThat(inr100.amount()).isEqualByComparingTo("100.13");
        assertThatThrownBy(() -> inr100.add(usd50))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void ownerAccountNaturallyModelsSingleAndMultiPropertyPortfolios() {
        OwnerAccount account = new OwnerAccount(
                "own-1",
                " coastal stays ",
                "host@coastal.in",
                "9999999999",
                Instant.parse("2026-11-22T00:00:00Z")
        );

        account.registerProperty("prop-1");
        assertThat(account.getPropertyCount()).isEqualTo(1);
        assertThat(account.isMultiPropertyPortfolio()).isFalse();

        account.registerProperty("prop-2");
        assertThat(account.getPropertyCount()).isEqualTo(2);
        assertThat(account.isMultiPropertyPortfolio()).isTrue();
        assertThat(account.ownsProperty("prop-1")).isTrue();
    }

    @Test
    void bookingEnforcesValidLifecycleTransitionsAndRejectsIllegalOnes() {
        Booking booking = sampleBooking();
        Instant now = Instant.parse("2026-11-22T10:00:00Z");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.isHoldingInventory()).isTrue();

        booking.confirmPayment("pay-101", now);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.isHoldingInventory()).isTrue();

        assertThatThrownBy(() -> booking.confirmPayment("pay-102", now))
                .isInstanceOf(InvalidStateTransitionException.class);

        booking.cancel(new CancellationDetails(
                now.plusSeconds(60),
                "Plans changed",
                "FLEXIBLE",
                Money.inr(5000),
                "RFD-1"
        ));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.isHoldingInventory()).isFalse();

        assertThatThrownBy(() -> booking.cancel(new CancellationDetails(
                now.plusSeconds(120),
                "Duplicate cancel",
                "FLEXIBLE",
                Money.inr(0),
                null
        ))).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void paymentFailedBookingIsTerminalAndCannotBeConfirmedOrCancelled() {
        Booking booking = sampleBooking();
        Instant now = Instant.parse("2026-11-22T10:00:00Z");

        booking.markPaymentFailed("pay-fail", now);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(booking.isHoldingInventory()).isFalse();

        assertThatThrownBy(() -> booking.confirmPayment("pay-retry", now))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> booking.cancel(new CancellationDetails(
                now, "Cancel failed booking", "NONE", Money.inr(0), null
        ))).isInstanceOf(InvalidStateTransitionException.class);
    }

    private Booking sampleBooking() {
        Money nightly = Money.inr(5000);
        PriceQuote quote = new PriceQuote(
                Money.inr(10000),
                List.of(),
                Money.inr(10000),
                nightly,
                2
        );
        return new Booking(
                "bkg-1",
                "prop-1",
                "room-1",
                new GuestDetails("Arjun Rao", "arjun@example.com", "9876543210"),
                new DateRange(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 3)),
                2,
                1,
                quote,
                Instant.parse("2026-11-22T09:00:00Z")
        );
    }
}
