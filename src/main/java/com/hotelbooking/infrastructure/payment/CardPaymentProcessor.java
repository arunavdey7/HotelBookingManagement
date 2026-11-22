package com.hotelbooking.infrastructure.payment;

import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.exception.ValidationException;
import com.hotelbooking.domain.payment.CardPaymentDetails;
import com.hotelbooking.domain.payment.GatewayChargeResult;
import com.hotelbooking.domain.payment.GatewayRefundResult;
import com.hotelbooking.domain.payment.Payment;
import com.hotelbooking.domain.payment.PaymentInstrumentDetails;
import com.hotelbooking.domain.payment.PaymentProcessor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Simulated card payment gateway adapter.
 * Deterministic test hook: card numbers ending with "0000" simulate an issuer decline.
 */
@Component
public class CardPaymentProcessor implements PaymentProcessor {

    @Override
    public String supportedMethod() {
        return CardPaymentDetails.METHOD;
    }

    @Override
    public GatewayChargeResult charge(
            String bookingId,
            Money amount,
            PaymentInstrumentDetails instrumentDetails
    ) {
        if (!(instrumentDetails instanceof CardPaymentDetails card)) {
            throw new ValidationException("Expected CardPaymentDetails for CARD payment method");
        }
        if ("0000".equals(card.lastFourDigits())) {
            return GatewayChargeResult.failed("Card declined by issuer: insufficient funds");
        }
        String reference = "CARD-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return GatewayChargeResult.succeeded(reference);
    }

    @Override
    public GatewayRefundResult refund(Payment payment, Money refundAmount) {
        String refundRef = "CARD-RFD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return GatewayRefundResult.succeeded(refundRef);
    }
}
