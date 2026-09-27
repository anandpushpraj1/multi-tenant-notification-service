package com.example.notification.ratelimit;

import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;

@Component
public class TenantRateLimiter {
    private static class Bucket {
        double tokens;
        long lastNanos;

        Bucket(int cap, long nowNanos) {
            tokens = cap;
            lastNanos = nowNanos;
        }
    }

    private final ConcurrentHashMap<UUID, Bucket> buckets = new ConcurrentHashMap<>();
    private final LongSupplier nanoTime;

    public TenantRateLimiter() {
        this(System::nanoTime);
    }

    TenantRateLimiter(LongSupplier nanoTime) {
        this.nanoTime = Objects.requireNonNull(nanoTime);
    }

    public boolean tryAcquire(UUID tenant, int rate) {
        if (rate <= 0) return false;
        Bucket b = buckets.computeIfAbsent(tenant, k -> new Bucket(rate, nanoTime.getAsLong()));
        synchronized (b) {
            long now = nanoTime.getAsLong();
            double elapsed = (now - b.lastNanos) / 1_000_000_000d;
            b.tokens = Math.min(rate, b.tokens + elapsed * rate);
            b.lastNanos = now;
            if (b.tokens >= 1) {
                b.tokens -= 1;
                return true;
            }

            return false;
        }
    }

    public void clear(UUID tenant) {
        buckets.remove(tenant);
    }
}
