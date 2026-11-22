package com.hotelbooking.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Map;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record ProcessPaymentRequest(
            @NotBlank(message = "Payment method is required (e.g., CARD, UPI, WALLET)")
            String method,
            Map<String, String> details,
            String idempotencyKey
    ) {
    }

    public record PaymentResponse(
            String paymentId,
            String bookingId,
            MoneyDto amount,
            String method,
            String paymentStatus,
            String gatewayReference,
            String failureReason,
            MoneyDto refundedAmount,
            String bookingStatus,
            boolean idempotentReplay,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
