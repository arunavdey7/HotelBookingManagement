package com.hotelbooking.api.mapper;

import com.hotelbooking.domain.payment.PaymentInstrumentDetails;

import java.util.Map;

public interface PaymentInstrumentParser {

    String supportedMethod();

    PaymentInstrumentDetails parse(Map<String, String> details);
}
