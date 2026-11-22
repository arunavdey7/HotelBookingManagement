package com.hotelbooking.domain.payment;

import com.hotelbooking.domain.common.Money;

/**
 * Pluggable payment gateway adapter port.
 * Adding support for a new payment method only requires registering a new implementation.
 */
public interface PaymentProcessor {

    String supportedMethod();

    GatewayChargeResult charge(String bookingId, Money amount, PaymentInstrumentDetails instrumentDetails);

    GatewayRefundResult refund(Payment payment, Money refundAmount);
}
