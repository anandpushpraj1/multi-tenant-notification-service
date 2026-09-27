package com.example.notification.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.notification.audit.AuditService;
import com.example.notification.channel.ChannelRegistry;
import com.example.notification.delivery.DeliveryAttemptRepository;
import com.example.notification.ratelimit.TenantRateLimiter;
import com.example.notification.tenant.Tenant;
import com.example.notification.tenant.TenantRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

class DispatchEngineTest {
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final TenantRepository tenants = mock(TenantRepository.class);
    private final DeliveryAttemptRepository attempts = mock(DeliveryAttemptRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final TenantRateLimiter limiter = mock(TenantRateLimiter.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ChannelRegistry channels = mock(ChannelRegistry.class);
    private DispatchEngine engine;

    @BeforeEach
    void setUp() {
        engine =
                new DispatchEngine(
                        notifications,
                        tenants,
                        attempts,
                        audit,
                        limiter,
                        notificationService,
                        channels);
    }

    @Test
    void usesExponentialBackoffAndCapsDelay() {
        assertEquals(Duration.ofSeconds(1), engine.backoff(1));
        assertEquals(Duration.ofSeconds(2), engine.backoff(2));
        assertEquals(Duration.ofSeconds(4), engine.backoff(3));
        assertEquals(Duration.ofSeconds(8), engine.backoff(4));
        assertEquals(Duration.ofSeconds(16), engine.backoff(5));
        assertEquals(Duration.ofSeconds(32), engine.backoff(6));
        assertEquals(Duration.ofSeconds(60), engine.backoff(7));
        assertEquals(Duration.ofSeconds(60), engine.backoff(8));
    }

    @Test
    void rotatesStartingTenantBetweenTicks() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        List<UUID> tenantIds = List.of(first, second, third);
        when(notifications.findEligibleTenantIds(anyCollection(), any(Instant.class)))
                .thenReturn(tenantIds);
        for (UUID tenantId : tenantIds) {
            when(tenants.findById(tenantId))
                    .thenReturn(Optional.of(new Tenant(tenantId.toString(), 1)));
            when(limiter.tryAcquire(tenantId, 1)).thenReturn(true);
            when(notifications.claimCandidates(
                            eq(tenantId),
                            anyCollection(),
                            any(Instant.class),
                            eq(PageRequest.of(0, 1))))
                    .thenReturn(List.of());
        }

        engine.tick();
        engine.tick();

        ArgumentCaptor<UUID> tenantCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(notifications, times(6))
                .claimCandidates(
                        tenantCaptor.capture(),
                        anyCollection(),
                        any(Instant.class),
                        eq(PageRequest.of(0, 1)));
        assertEquals(
                List.of(first, second, third, second, third, first), tenantCaptor.getAllValues());
    }
}
