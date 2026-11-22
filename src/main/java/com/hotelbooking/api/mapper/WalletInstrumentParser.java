package com.hotelbooking.api.mapper;

import com.hotelbooking.domain.exception.ValidationException;
import com.hotelbooking.domain.payment.PaymentInstrumentDetails;
import com.hotelbooking.domain.payment.WalletPaymentDetails;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WalletInstrumentParser implements PaymentInstrumentParser {

    @Override
    public String supportedMethod() {
        return WalletPaymentDetails.METHOD;
    }

    @Override
    public PaymentInstrumentDetails parse(Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            throw new ValidationException("WALLET payment details (walletProvider, walletAccountId) are required");
        }
        return new WalletPaymentDetails(
                details.get("walletProvider"),
                details.get("walletAccountId")
        );
    }
}
