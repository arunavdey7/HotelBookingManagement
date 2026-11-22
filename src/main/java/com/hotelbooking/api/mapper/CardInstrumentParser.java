package com.hotelbooking.api.mapper;

import com.hotelbooking.domain.exception.ValidationException;
import com.hotelbooking.domain.payment.CardPaymentDetails;
import com.hotelbooking.domain.payment.PaymentInstrumentDetails;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CardInstrumentParser implements PaymentInstrumentParser {

    @Override
    public String supportedMethod() {
        return CardPaymentDetails.METHOD;
    }

    @Override
    public PaymentInstrumentDetails parse(Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            throw new ValidationException("CARD payment details (cardNumber, cardHolderName, expiryMonthYear, cvv) are required");
        }
        return new CardPaymentDetails(
                details.get("cardNumber"),
                details.get("cardHolderName"),
                details.get("expiryMonthYear"),
                details.get("cvv")
        );
    }
}
