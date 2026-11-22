package com.hotelbooking.domain.payment;

import java.time.Instant;
import java.util.Optional;

public record IdempotencyRecord(
        String idempotencyKey,
        String requestFingerprint,
        String paymentId,
        Instant createdAt
) {

    public interface Repository {
        Optional<IdempotencyRecord> findByKey(String idempotencyKey);

        IdempotencyRecord save(IdempotencyRecord record);
    }
}
