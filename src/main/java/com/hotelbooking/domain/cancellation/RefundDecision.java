package com.hotelbooking.domain.cancellation;

import com.hotelbooking.domain.common.Money;

import java.math.BigDecimal;

public record RefundDecision(
        Money refundAmount,
        BigDecimal refundPercentage,
        String policyCode,
        String explanation
) {
}
