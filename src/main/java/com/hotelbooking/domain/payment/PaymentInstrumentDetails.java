package com.hotelbooking.domain.payment;

/**
 * Open polymorphism contract for payment method instruments.
 * New payment instruments (e.g., NET_BANKING, BNPL) implement this interface without
 * modifying existing payment domain types.
 */
public interface PaymentInstrumentDetails {

    String methodType();

    String fingerprintSummary();
}
