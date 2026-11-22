package com.hotelbooking.infrastructure.payment;

import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.exception.ValidationException;
import com.hotelbooking.domain.payment.GatewayChargeResult;
import com.hotelbooking.domain.payment.GatewayRefundResult;
import com.hotelbooking.domain.payment.Payment;
import com.hotelbooking.domain.payment.PaymentInstrumentDetails;
import com.hotelbooking.domain.payment.PaymentProcessor;
import com.hotelbooking.domain.payment.WalletPaymentDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Simulated digital wallet gateway adapter.
 * Deterministic test hook: walletAccountId containing "insufficient" simulates low wallet balance.
 */
@Component
public class WalletPaymentProcessor implements PaymentProcessor {

    @Override
    public String supportedMethod() {
        return WalletPaymentDetails.METHOD;
    }

    @Override
    public GatewayChargeResult charge(
            String bookingId,
            Money amount,
            PaymentInstrumentDetails instrumentDetails
    ) {
        if (!(instrumentDetails instanceof WalletPaymentDetails wallet)) {
            throw new ValidationException("Expected WalletPaymentDetails for WALLET payment method");
        }
        if (wallet.walletAccountId().toLowerCase().contains("insufficient")) {
            return GatewayChargeResult.failed("Wallet debit failed: insufficient wallet balance");
        }
        String reference = "WLT-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return GatewayChargeResult.succeeded(reference);
    }

    @Override
    public GatewayRefundResult refund(Payment payment, Money refundAmount) {
        String refundRef = "WLT-RFD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return GatewayRefundResult.succeeded(refundRef);
    }
}
