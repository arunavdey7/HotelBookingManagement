package com.hotelbooking.domain.payment;

public record GatewayRefundResult(
        boolean success,
        String refundReference,
        String failureReason
) {

    public static GatewayRefundResult succeeded(String refundReference) {
        return new GatewayRefundResult(true, refundReference, null);
    }

    public static GatewayRefundResult failed(String failureReason) {
        return new GatewayRefundResult(false, null, failureReason);
    }
}
