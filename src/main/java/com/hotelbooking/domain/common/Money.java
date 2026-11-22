package com.hotelbooking.domain.common;

import com.hotelbooking.domain.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    public static final Currency DEFAULT_CURRENCY = Currency.getInstance("INR");

    public Money {
        if (amount == null) {
            throw new ValidationException("Money amount cannot be null");
        }
        if (currency == null) {
            throw new ValidationException("Currency cannot be null");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return new Money(amount, DEFAULT_CURRENCY);
        }
        try {
            return new Money(amount, Currency.getInstance(currencyCode.trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Unsupported ISO currency code: " + currencyCode);
        }
    }

    public static Money of(double amount, String currencyCode) {
        return of(BigDecimal.valueOf(amount), currencyCode);
    }

    public static Money inr(BigDecimal amount) {
        return new Money(amount, DEFAULT_CURRENCY);
    }

    public static Money inr(double amount) {
        return new Money(BigDecimal.valueOf(amount), DEFAULT_CURRENCY);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "Multiplier cannot be null");
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public Money divide(int divisor) {
        if (divisor <= 0) {
            throw new ValidationException("Divisor must be positive");
        }
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP), this.currency);
    }

    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) <= 0;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "Other money operand cannot be null");
        if (!this.currency.equals(other.currency)) {
            throw new ValidationException(
                    "Currency mismatch: cannot combine " + this.currency + " and " + other.currency
            );
        }
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }
}
