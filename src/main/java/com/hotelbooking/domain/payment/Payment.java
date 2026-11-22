package com.hotelbooking.domain.payment;

import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.exception.InvalidStateTransitionException;
import com.hotelbooking.domain.exception.ValidationException;

import java.time.Instant;
import java.util.Objects;

public class Payment {

    private final String id;
    private final String bookingId;
    private final Money amount;
    private final String methodType;
    private final String idempotencyKey;
    private PaymentStatus status;
    private String gatewayReference;
    private String failureReason;
    private Money refundedAmount;
    private final Instant createdAt;
    private Instant updatedAt;

    public Payment(
            String id,
            String bookingId,
            Money amount,
            String methodType,
            String idempotencyKey,
            Instant createdAt
    ) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("Payment id cannot be blank");
        }
        if (bookingId == null || bookingId.isBlank()) {
            throw new ValidationException("Booking id is required for payment");
        }
        if (amount == null || !amount.isPositive()) {
            throw new ValidationException("Payment amount must be positive");
        }
        if (methodType == null || methodType.isBlank()) {
            throw new ValidationException("Payment methodType is required");
        }
        this.id = id.trim();
        this.bookingId = bookingId.trim();
        this.amount = amount;
        this.methodType = methodType.trim().toUpperCase();
        this.idempotencyKey = (idempotencyKey == null || idempotencyKey.isBlank())
                ? null
                : idempotencyKey.trim();
        this.status = PaymentStatus.INITIATED;
        this.refundedAmount = Money.zero(amount.currency());
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = createdAt;
    }

    public synchronized void markSucceeded(String gatewayReference, Instant timestamp) {
        if (this.status != PaymentStatus.INITIATED) {
            throw new InvalidStateTransitionException("Cannot mark payment SUCCEEDED from " + this.status);
        }
        this.status = PaymentStatus.SUCCEEDED;
        this.gatewayReference = Objects.requireNonNull(gatewayReference, "gatewayReference cannot be null");
        this.updatedAt = Objects.requireNonNull(timestamp, "timestamp cannot be null");
    }

    public synchronized void markFailed(String reason, Instant timestamp) {
        if (this.status != PaymentStatus.INITIATED) {
            throw new InvalidStateTransitionException("Cannot mark payment FAILED from " + this.status);
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason == null ? "Payment declined" : reason;
        this.updatedAt = Objects.requireNonNull(timestamp, "timestamp cannot be null");
    }

    public synchronized void recordRefund(Money refundAmount, Instant timestamp) {
        if (this.status != PaymentStatus.SUCCEEDED && this.status != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new InvalidStateTransitionException("Cannot refund payment in state " + this.status);
        }
        Objects.requireNonNull(refundAmount, "refundAmount cannot be null");
        if (refundAmount.isNegative()) {
            throw new ValidationException("Refund amount cannot be negative");
        }
        if (refundAmount.isZero()) {
            return;
        }
        Money cumulative = this.refundedAmount.add(refundAmount);
        if (cumulative.compareTo(this.amount) > 0) {
            throw new ValidationException("Cumulative refund cannot exceed paid amount");
        }
        this.refundedAmount = cumulative;
        this.status = cumulative.compareTo(this.amount) == 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
        this.updatedAt = Objects.requireNonNull(timestamp, "timestamp cannot be null");
    }

    public String getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public Money getAmount() {
        return amount;
    }

    public String getMethodType() {
        return methodType;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public synchronized PaymentStatus getStatus() {
        return status;
    }

    public synchronized String getGatewayReference() {
        return gatewayReference;
    }

    public synchronized String getFailureReason() {
        return failureReason;
    }

    public synchronized Money getRefundedAmount() {
        return refundedAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public synchronized Instant getUpdatedAt() {
        return updatedAt;
    }
}
