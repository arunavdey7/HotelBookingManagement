package com.hotelbooking.application;

import com.hotelbooking.application.cancellation.CancellationProperties;
import com.hotelbooking.application.cancellation.FlexibleCancellationPolicy;
import com.hotelbooking.application.cancellation.NonRefundableCancellationPolicy;
import com.hotelbooking.application.cancellation.TieredCancellationPolicy;
import com.hotelbooking.application.pricing.DynamicPricingStrategy;
import com.hotelbooking.application.pricing.FlatRatePricingStrategy;
import com.hotelbooking.application.pricing.LongStayDiscountPricingRule;
import com.hotelbooking.application.pricing.PricingProperties;
import com.hotelbooking.application.pricing.WeekendSurgePricingRule;
import com.hotelbooking.domain.booking.Booking;
import com.hotelbooking.domain.booking.GuestDetails;
import com.hotelbooking.domain.cancellation.RefundDecision;
import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.common.Location;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.pricing.PriceQuote;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.PropertyType;
import com.hotelbooking.domain.property.RoomCategory;
import com.hotelbooking.domain.property.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PricingAndCancellationPolicyTest {

    private final RoomType sampleRoom = new RoomType(
            "room-deluxe",
            "Deluxe Room",
            RoomCategory.DELUXE,
            2,
            3,
            Money.inr(4000),
            Set.of()
    );

    private final Property sampleProperty = new Property(
            "prop-1",
            "own-1",
            "Test Hotel",
            "Desc",
            PropertyType.HOTEL,
            new Location("1 MG Road", "Indiranagar", "Bengaluru", "KA", "India", "560038"),
            4,
            Set.of(),
            List.of(sampleRoom),
            "MODERATE_TIERED",
            Instant.parse("2026-11-22T00:00:00Z")
    );

    @Test
    void flatRatePricingMultipliesNightlyBaseRateByNights() {
        FlatRatePricingStrategy flat = new FlatRatePricingStrategy();
        // Friday 2027-01-08 to Sunday 2027-01-10 (2 weekend nights)
        DateRange weekendStay = new DateRange(LocalDate.of(2027, 1, 8), LocalDate.of(2027, 1, 10));

        PriceQuote quote = flat.quote(sampleProperty, sampleRoom, weekendStay, 2);

        assertThat(quote.baseAmount().amount()).isEqualByComparingTo("8000.00");
        assertThat(quote.totalAmount().amount()).isEqualByComparingTo("8000.00");
        assertThat(quote.adjustments()).isEmpty();
    }

    @Test
    void dynamicPricingAppliesWeekendSurgeAndLongStayDiscount() {
        PricingProperties props = new PricingProperties(
                "dynamic",
                new BigDecimal("1.20"),
                7,
                new BigDecimal("0.10")
        );
        DynamicPricingStrategy dynamic = new DynamicPricingStrategy(List.of(
                new WeekendSurgePricingRule(props),
                new LongStayDiscountPricingRule(props)
        ));

        // 7 nights starting Monday 2027-01-04 to Monday 2027-01-11 (includes Fri Jan 8 + Sat Jan 9)
        // Base = 7 * 4000 = 28000
        // Weekend surge = 2 * (4000 * 0.20) = +1600 -> subtotal 29600
        // Long stay 10% discount on 29600 = -2960 -> final total 26640
        DateRange sevenNightStay = new DateRange(LocalDate.of(2027, 1, 4), LocalDate.of(2027, 1, 11));

        PriceQuote quote = dynamic.quote(sampleProperty, sampleRoom, sevenNightStay, 2);

        assertThat(quote.baseAmount().amount()).isEqualByComparingTo("28000.00");
        assertThat(quote.adjustments()).hasSize(2);
        assertThat(quote.totalAmount().amount()).isEqualByComparingTo("26640.00");
    }

    @Test
    void tieredCancellationPolicyAppliesFullHalfAndZeroRefundWindows() {
        CancellationProperties props = new CancellationProperties("MODERATE_TIERED", 14);
        TieredCancellationPolicy policy = new TieredCancellationPolicy(props);

        // Check-in is 2027-03-10 at 14:00 UTC, total paid = 10,000 INR
        Booking booking = new Booking(
                "bkg-100",
                "prop-1",
                "room-deluxe",
                new GuestDetails("Meera Joshi", "meera@example.com", "9000000000"),
                new DateRange(LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12)),
                2,
                1,
                new PriceQuote(Money.inr(10000), List.of(), Money.inr(10000), Money.inr(5000), 2),
                Instant.parse("2027-03-01T10:00:00Z")
        );

        // 72 hours before check-in -> 100% refund
        RefundDecision earlyDecision = policy.evaluateRefund(
                booking,
                Instant.parse("2027-03-07T14:00:00Z")
        );
        assertThat(earlyDecision.refundAmount().amount()).isEqualByComparingTo("10000.00");
        assertThat(earlyDecision.refundPercentage()).isEqualByComparingTo("100.00");

        // 30 hours before check-in -> 50% refund
        RefundDecision midDecision = policy.evaluateRefund(
                booking,
                Instant.parse("2027-03-09T08:00:00Z")
        );
        assertThat(midDecision.refundAmount().amount()).isEqualByComparingTo("5000.00");
        assertThat(midDecision.refundPercentage()).isEqualByComparingTo("50.00");

        // 12 hours before check-in -> 0% refund
        RefundDecision lateDecision = policy.evaluateRefund(
                booking,
                Instant.parse("2027-03-10T02:00:00Z")
        );
        assertThat(lateDecision.refundAmount().amount()).isEqualByComparingTo("0.00");
        assertThat(lateDecision.refundPercentage()).isEqualByComparingTo("0.00");
    }

    @Test
    void flexibleAndNonRefundablePoliciesBehaveDeterministically() {
        CancellationProperties props = new CancellationProperties("FLEXIBLE", 14);
        FlexibleCancellationPolicy flexible = new FlexibleCancellationPolicy(props);
        NonRefundableCancellationPolicy nonRefundable = new NonRefundableCancellationPolicy();

        Booking booking = new Booking(
                "bkg-101",
                "prop-1",
                "room-deluxe",
                new GuestDetails("Vikram Singh", "vikram@example.com", "9000000001"),
                new DateRange(LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12)),
                2,
                1,
                new PriceQuote(Money.inr(6000), List.of(), Money.inr(6000), Money.inr(3000), 2),
                Instant.parse("2027-03-01T10:00:00Z")
        );

        // Even 2 hours before check-in, FLEXIBLE grants 100% refund
        Instant twoHoursBefore = Instant.parse("2027-03-10T12:00:00Z");
        assertThat(flexible.evaluateRefund(booking, twoHoursBefore).refundAmount().amount())
                .isEqualByComparingTo("6000.00");

        // NON_REFUNDABLE always grants 0% even weeks earlier
        Instant weeksBefore = Instant.parse("2027-03-02T12:00:00Z");
        assertThat(nonRefundable.evaluateRefund(booking, weeksBefore).refundAmount().amount())
                .isEqualByComparingTo("0.00");
    }
}
