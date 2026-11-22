package com.hotelbooking.infrastructure.payment;

import com.hotelbooking.domain.exception.PaymentProcessingException;
import com.hotelbooking.domain.payment.PaymentProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentProcessorRegistry {

    private final Map<String, PaymentProcessor> processorsByMethod;

    public PaymentProcessorRegistry(List<PaymentProcessor> processors) {
        this.processorsByMethod = processors.stream()
                .collect(Collectors.toUnmodifiableMap(
                        processor -> processor.supportedMethod().trim().toUpperCase(),
                        Function.identity()
                ));
    }

    public PaymentProcessor getProcessor(String methodType) {
        if (methodType == null || methodType.isBlank()) {
            throw new PaymentProcessingException("Payment method is required");
        }
        PaymentProcessor processor = processorsByMethod.get(methodType.trim().toUpperCase());
        if (processor == null) {
            throw new PaymentProcessingException(
                    "Unsupported payment method: " + methodType + ". Supported methods: " + supportedMethods()
            );
        }
        return processor;
    }

    public Set<String> supportedMethods() {
        return processorsByMethod.keySet();
    }
}
