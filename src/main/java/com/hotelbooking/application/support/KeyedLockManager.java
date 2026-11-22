package com.hotelbooking.application.support;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Provides fine-grained striped/keyed locks so state transitions on the same booking
 * or idempotency key execute atomically without global contention.
 */
@Component
public class KeyedLockManager {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T executeWithLock(String scope, String key, Supplier<T> action) {
        String compositeKey = scope + ":" + key;
        ReentrantLock lock = locks.computeIfAbsent(compositeKey, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
