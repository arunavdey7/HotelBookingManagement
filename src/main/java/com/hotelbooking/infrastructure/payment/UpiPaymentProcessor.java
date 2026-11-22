package com.hotelbooking.infrastructure.payment;

import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.exception.ValidationException;
import com.hotelbooking.domain.payment.GatewayChargeResult;
import com.hotelbooking.domain.payment.GatewayRefundResult;
import com.hotelbooking.domain.payment.Payment;
import com.hotelbooking.domain.payment.PaymentInstrumentDetails;
import com.hotelbooking.domain.payment.PaymentProcessor;
import com.hotelbooking.domain.payment.UpiPaymentDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Simulated UPI gateway adapter.
 * Deterministic test hook: VPAs starting with "fail@" or "declined@" simulate collect rejection.
 */
@Component
public class UpiPaymentProcessor implements PaymentProcessor {

    @Override
    public String supportedMethod() {
        return UpiPaymentDetails.METHOD;
    }

    @Override
    public GatewayChargeResult charge(
            String bookingId,
            Money amount,
            PaymentInstrumentDetails instrumentDetails
    ) {
        if (!(instrumentDetails instanceof UpiPaymentDetails upi)) {
            throw new ValidationException("Expected UpiPaymentDetails for UPI payment method");
        }
        if (upi.vpa().startsWith("fail@") || upi.vpa().startsWith("declined@")) {
            return GatewayChargeResult.failed("UPI collect request declined by user or bank");
        }
        String reference = "UPI-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return GatewayChargeResult.succeeded(reference);
    }

    @Override
    public GatewayRefundResult refund(Payment payment, Money refundAmount) {
        String refundRef = "UPI-RFD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return GatewayRefundResult.succeeded(refundRef);
    }
}
