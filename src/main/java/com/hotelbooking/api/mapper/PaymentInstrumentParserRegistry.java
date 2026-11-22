package com.hotelbooking.api.mapper;

import com.hotelbooking.domain.exception.PaymentProcessingException;
import com.hotelbooking.domain.payment.PaymentInstrumentDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentInstrumentParserRegistry {

    private final Map<String, PaymentInstrumentParser> parsersByMethod;

    public PaymentInstrumentParserRegistry(List<PaymentInstrumentParser> parsers) {
        this.parsersByMethod = parsers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        parser -> parser.supportedMethod().trim().toUpperCase(),
                        Function.identity()
                ));
    }

    public PaymentInstrumentDetails parse(String method, Map<String, String> details) {
        if (method == null || method.isBlank()) {
            throw new PaymentProcessingException("Payment method is required");
        }
        PaymentInstrumentParser parser = parsersByMethod.get(method.trim().toUpperCase());
        if (parser == null) {
            throw new PaymentProcessingException(
                    "Unsupported payment method: " + method + ". Supported methods: " + parsersByMethod.keySet()
            );
        }
        return parser.parse(details);
    }
}
