package com.example.notification.ratelimit;
import org.junit.jupiter.api.Test; import java.util.*; import static org.junit.jupiter.api.Assertions.*;
class TenantRateLimiterTest{@Test void capacityIsEnforced(){var r=new TenantRateLimiter();UUID t=UUID.randomUUID();assertTrue(r.tryAcquire(t,2));assertTrue(r.tryAcquire(t,2));assertFalse(r.tryAcquire(t,2));}}
