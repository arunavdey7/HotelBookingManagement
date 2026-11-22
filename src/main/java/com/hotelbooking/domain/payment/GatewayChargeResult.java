package com.hotelbooking.domain.payment;

public record GatewayChargeResult(
        boolean success,
        String transactionReference,
        String failureReason
) {

    public static GatewayChargeResult succeeded(String transactionReference) {
        return new GatewayChargeResult(true, transactionReference, null);
    }

    public static GatewayChargeResult failed(String failureReason) {
        return new GatewayChargeResult(false, null, failureReason);
    }
}
