package com.hotelbooking.application.cancellation;

import com.hotelbooking.domain.booking.Booking;
import com.hotelbooking.domain.cancellation.CancellationPolicy;
import com.hotelbooking.domain.cancellation.RefundDecision;
import com.hotelbooking.domain.common.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class NonRefundableCancellationPolicy implements CancellationPolicy {

    public static final String CODE = "NON_REFUNDABLE";

    @Override
    public String policyCode() {
        return CODE;
    }

    @Override
    public RefundDecision evaluateRefund(Booking booking, Instant cancelledAt) {
        Money paidTotal = booking.getPriceQuote().totalAmount();
        return new RefundDecision(
                Money.zero(paidTotal.currency()),
                BigDecimal.ZERO.setScale(2),
                CODE,
                "0% refund: non-refundable rate policy"
        );
    }
}
