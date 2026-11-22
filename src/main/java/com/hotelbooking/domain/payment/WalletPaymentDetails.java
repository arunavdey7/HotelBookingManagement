package com.hotelbooking.domain.payment;

import com.hotelbooking.domain.exception.ValidationException;

public record WalletPaymentDetails(String walletProvider, String walletAccountId) implements PaymentInstrumentDetails {

    public static final String METHOD = "WALLET";

    public WalletPaymentDetails {
        if (walletProvider == null || walletProvider.isBlank()) {
            throw new ValidationException("Wallet provider is required");
        }
        if (walletAccountId == null || walletAccountId.isBlank()) {
            throw new ValidationException("Wallet account identifier is required");
        }
        walletProvider = walletProvider.trim().toUpperCase();
        walletAccountId = walletAccountId.trim();
    }

    @Override
    public String methodType() {
        return METHOD;
    }

    @Override
    public String fingerprintSummary() {
        return METHOD + ":" + walletProvider + ":" + walletAccountId;
    }
}
