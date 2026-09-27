package com.example.notification.ratelimit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

class TenantRateLimiterTest {
    @Test
    void capacityIsEnforced() {
        var r = new TenantRateLimiter();
        UUID t = UUID.randomUUID();
        assertTrue(r.tryAcquire(t, 2));
        assertTrue(r.tryAcquire(t, 2));
        assertFalse(r.tryAcquire(t, 2));
    }

    @Test
    void refillsTokensAtConfiguredRate() {
        AtomicLong clock = new AtomicLong();
        TenantRateLimiter limiter = new TenantRateLimiter(clock::get);
        UUID tenant = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(tenant, 2));
        assertTrue(limiter.tryAcquire(tenant, 2));
        assertFalse(limiter.tryAcquire(tenant, 2));

        clock.addAndGet(500_000_000L);

        assertTrue(limiter.tryAcquire(tenant, 2));
        assertFalse(limiter.tryAcquire(tenant, 2));
    }

    @Test
    void capsRefillAtBucketCapacity() {
        AtomicLong clock = new AtomicLong();
        TenantRateLimiter limiter = new TenantRateLimiter(clock::get);
        UUID tenant = UUID.randomUUID();

        clock.addAndGet(10_000_000_000L);

        assertTrue(limiter.tryAcquire(tenant, 2));
        assertTrue(limiter.tryAcquire(tenant, 2));
        assertFalse(limiter.tryAcquire(tenant, 2));
    }

    @Test
    void tenantsHaveIndependentBuckets() {
        TenantRateLimiter limiter = new TenantRateLimiter(() -> 0L);
        UUID firstTenant = UUID.randomUUID();
        UUID secondTenant = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(firstTenant, 1));
        assertFalse(limiter.tryAcquire(firstTenant, 1));
        assertTrue(limiter.tryAcquire(secondTenant, 1));
    }

    @Test
    void rejectsNonPositiveRates() {
        TenantRateLimiter limiter = new TenantRateLimiter(() -> 0L);

        assertFalse(limiter.tryAcquire(UUID.randomUUID(), 0));
        assertFalse(limiter.tryAcquire(UUID.randomUUID(), -1));
    }

    @Test
    void clearCreatesANewFullBucket() {
        TenantRateLimiter limiter = new TenantRateLimiter(() -> 0L);
        UUID tenant = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(tenant, 1));
        assertFalse(limiter.tryAcquire(tenant, 1));

        limiter.clear(tenant);

        assertTrue(limiter.tryAcquire(tenant, 1));
    }

    @Test
    void concurrentAcquisitionsDoNotExceedBucketCapacity() throws Exception {
        int capacity = 32;
        int threadCount = 16;
        int attemptsPerThread = 8;
        TenantRateLimiter limiter = new TenantRateLimiter(() -> 0L);
        UUID tenant = UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int thread = 0; thread < threadCount; thread++) {
                results.add(
                        executor.submit(
                                () -> {
                                    ready.countDown();
                                    if (!start.await(5, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException("Start signal timed out");
                                    }
                                    int acquired = 0;
                                    for (int attempt = 0; attempt < attemptsPerThread; attempt++) {
                                        if (limiter.tryAcquire(tenant, capacity)) acquired++;
                                    }
                                    return acquired;
                                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int acquired = 0;
            for (Future<Integer> result : results) {
                acquired += result.get(5, TimeUnit.SECONDS);
            }
            assertEquals(capacity, acquired);
        } finally {
            executor.shutdownNow();
        }
    }
}
