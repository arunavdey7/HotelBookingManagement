package com.hotelbooking.domain.payment;

import com.hotelbooking.domain.exception.ValidationException;

public record CardPaymentDetails(
        String cardNumber,
        String cardHolderName,
        String expiryMonthYear,
        String cvv
) implements PaymentInstrumentDetails {

    public static final String METHOD = "CARD";

    public CardPaymentDetails {
        if (cardNumber == null || !cardNumber.replaceAll("\\s+", "").matches("\\d{12,19}")) {
            throw new ValidationException("Card number must contain 12 to 19 digits");
        }
        if (cardHolderName == null || cardHolderName.isBlank()) {
            throw new ValidationException("Card holder name is required");
        }
        if (expiryMonthYear == null || !expiryMonthYear.trim().matches("(0[1-9]|1[0-2])/\\d{2,4}")) {
            throw new ValidationException("Card expiry must be in MM/YY or MM/YYYY format");
        }
        if (cvv == null || !cvv.trim().matches("\\d{3,4}")) {
            throw new ValidationException("CVV must be 3 or 4 digits");
        }
        cardNumber = cardNumber.replaceAll("\\s+", "");
        cardHolderName = cardHolderName.trim();
        expiryMonthYear = expiryMonthYear.trim();
        cvv = cvv.trim();
    }

    public String lastFourDigits() {
        return cardNumber.substring(cardNumber.length() - 4);
    }

    @Override
    public String methodType() {
        return METHOD;
    }

    @Override
    public String fingerprintSummary() {
        return METHOD + ":" + lastFourDigits();
    }
}
