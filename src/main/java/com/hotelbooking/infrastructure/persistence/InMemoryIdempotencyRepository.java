package com.hotelbooking.infrastructure.persistence;

import com.hotelbooking.domain.payment.IdempotencyRecord;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryIdempotencyRepository implements IdempotencyRecord.Repository {

    private final Map<String, IdempotencyRecord> store = new ConcurrentHashMap<>();

    @Override
    public Optional<IdempotencyRecord> findByKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(idempotencyKey.trim()));
    }

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        Objects.requireNonNull(record, "IdempotencyRecord cannot be null");
        store.put(record.idempotencyKey(), record);
        return record;
    }
}
