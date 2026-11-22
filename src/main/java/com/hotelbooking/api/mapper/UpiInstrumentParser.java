package com.hotelbooking.api.mapper;

import com.hotelbooking.domain.exception.ValidationException;
import com.hotelbooking.domain.payment.PaymentInstrumentDetails;
import com.hotelbooking.domain.payment.UpiPaymentDetails;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UpiInstrumentParser implements PaymentInstrumentParser {

    @Override
    public String supportedMethod() {
        return UpiPaymentDetails.METHOD;
    }

    @Override
    public PaymentInstrumentDetails parse(Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            throw new ValidationException("UPI payment details (vpa) are required");
        }
        return new UpiPaymentDetails(details.get("vpa"));
    }
}
