package com.hotelbooking.domain.payment;

import com.hotelbooking.domain.exception.ValidationException;

public record UpiPaymentDetails(String vpa) implements PaymentInstrumentDetails {

    public static final String METHOD = "UPI";

    public UpiPaymentDetails {
        if (vpa == null || !vpa.trim().matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$")) {
            throw new ValidationException("Invalid UPI VPA format (expected handle@bank)");
        }
        vpa = vpa.trim().toLowerCase();
    }

    @Override
    public String methodType() {
        return METHOD;
    }

    @Override
    public String fingerprintSummary() {
        return METHOD + ":" + vpa;
    }
}
