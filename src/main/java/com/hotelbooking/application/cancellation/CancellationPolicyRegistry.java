package com.hotelbooking.application.cancellation;

import com.hotelbooking.domain.cancellation.CancellationPolicy;
import com.hotelbooking.domain.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CancellationPolicyRegistry {

    private final Map<String, CancellationPolicy> policiesByCode;
    private final String defaultPolicyCode;

    public CancellationPolicyRegistry(
            List<CancellationPolicy> policies,
            CancellationProperties properties
    ) {
        this.policiesByCode = policies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        policy -> policy.policyCode().trim().toUpperCase(),
                        Function.identity()
                ));
        this.defaultPolicyCode = properties.defaultPolicy();
    }

    public CancellationPolicy resolve(String policyCode) {
        String key = (policyCode == null || policyCode.isBlank())
                ? defaultPolicyCode
                : policyCode.trim().toUpperCase();
        CancellationPolicy policy = policiesByCode.get(key);
        if (policy == null) {
            throw new ValidationException(
                    "Unsupported cancellation policy: " + key + ". Supported policies: " + supportedPolicyCodes()
            );
        }
        return policy;
    }

    public Set<String> supportedPolicyCodes() {
        return policiesByCode.keySet();
    }
}
