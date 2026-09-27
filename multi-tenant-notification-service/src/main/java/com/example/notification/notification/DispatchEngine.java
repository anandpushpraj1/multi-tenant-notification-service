package com.example.notification.notification;

import com.example.notification.audit.*;
import com.example.notification.channel.*;
import com.example.notification.delivery.*;
import com.example.notification.ratelimit.TenantRateLimiter;
import com.example.notification.tenant.*;

import jakarta.annotation.PreDestroy;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DispatchEngine {

    private final NotificationRepository notifications;
    private final TenantRepository tenants;
    private final DeliveryAttemptRepository attempts;
    private final AuditService audit;
    private final TenantRateLimiter limiter;
    private final NotificationService notificationService;
    private final ChannelRegistry channels;
    private final ExecutorService workers;
    private final AtomicInteger cursor = new AtomicInteger();
    private final int maxAttempts = 5;
    private final Duration initialBackoff = Duration.ofSeconds(1);
    private final Duration maxBackoff = Duration.ofSeconds(60);

    public DispatchEngine(
            NotificationRepository notifications,
            TenantRepository tenants,
            DeliveryAttemptRepository attempts,
            AuditService audit,
            TenantRateLimiter limiter,
            NotificationService notificationService,
            ChannelRegistry channels) {
        this.notifications = notifications;
        this.tenants = tenants;
        this.attempts = attempts;
        this.audit = audit;
        this.limiter = limiter;
        this.notificationService = notificationService;
        this.channels = channels;
        this.workers =
                new ThreadPoolExecutor(
                        8,
                        20,
                        30,
                        TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(1000),
                        new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.dispatcher.interval-ms:500}")
    public void tick() {
        notifications.promoteScheduled(Instant.now());
        List<UUID> tenantIds =
                notifications.findEligibleTenantIds(
                        List.of(NotificationStatus.QUEUED, NotificationStatus.RETRY_PENDING),
                        Instant.now());
        if (tenantIds.isEmpty()) return;
        int start = Math.floorMod(cursor.getAndIncrement(), tenantIds.size());
        for (int i = 0; i < tenantIds.size(); i++) {
            UUID tid = tenantIds.get((start + i) % tenantIds.size());
            Tenant t = tenants.findById(tid).orElse(null);
            if (t == null || t.getStatus() != TenantStatus.ACTIVE) continue;
            if (!limiter.tryAcquire(tid, t.getRateLimitPerSecond())) continue;

            Notification n = claimOne(tid);
            if (n != null) workers.submit(() -> deliver(n));
        }
    }

    @Transactional
    public Notification claimOne(UUID tenantId) {
        List<Notification> list =
                notifications.claimCandidates(
                        tenantId,
                        List.of(NotificationStatus.QUEUED, NotificationStatus.RETRY_PENDING),
                        Instant.now(),
                        PageRequest.of(0, 1));
        if (list.isEmpty()) return null;
        Notification n = list.get(0);
        String old = n.getStatus().name();
        n.processing();
        Notification saved = notifications.save(n);
        audit.transition(tenantId, n.getId(), old, n.getStatus().name(), "claimed");
        return saved;
    }

    private void deliver(Notification n) {
        Instant start = Instant.now();
        int attemptNo = n.incrementAttempt();
        try {
            var payload = notificationService.renderPayload(n);
            DeliveryResult result =
                    channels.get(n.getChannel())
                            .send(
                                    n,
                                    n.getChannel() == ChannelType.EMAIL
                                            ? (String) payload.get("subject")
                                            : null,
                                    (String) payload.get("body"));
            Instant end = Instant.now();
            attempts.save(
                    new DeliveryAttempt(
                            n.getId(),
                            attemptNo,
                            result.success()
                                    ? DeliveryAttemptStatus.SUCCESS
                                    : (result.transientFailure()
                                            ? DeliveryAttemptStatus.TRANSIENT_FAILURE
                                            : DeliveryAttemptStatus.PERMANENT_FAILURE),
                            result.providerResponse(),
                            result.errorCode(),
                            result.errorMessage(),
                            start,
                            end));
            audit.event(
                    n.getTenantId(),
                    n.getId(),
                    AuditEventType.DELIVERY_ATTEMPT,
                    "attempt=" + attemptNo + ",status=" + result);
            if (result.success()) {
                transition(n, NotificationStatus.SENT, "delivery-success");
            } else if (result.transientFailure() && attemptNo < maxAttempts) {
                Duration delay = backoff(attemptNo);
                transitionRetry(n, delay, "retry-in=" + delay.toSeconds() + "s");
            } else {
                transition(
                        n,
                        NotificationStatus.FAILED,
                        result.transientFailure() ? "max-attempts" : "permanent-failure");
            }
        } catch (Exception e) {
            attempts.save(
                    new DeliveryAttempt(
                            n.getId(),
                            attemptNo,
                            DeliveryAttemptStatus.TRANSIENT_FAILURE,
                            null,
                            "UNEXPECTED",
                            e.getMessage(),
                            start,
                            Instant.now()));
            if (attemptNo < maxAttempts) {
                Duration d = backoff(attemptNo);
                transitionRetry(n, d, "exception-retry");
            } else transition(n, NotificationStatus.FAILED, "max-attempts");
        }
    }

    private synchronized void transition(
            Notification n, NotificationStatus target, String metadata) {
        String old = n.getStatus().name();
        switch (target) {
            case SENT -> n.sent();
            case FAILED -> n.failed();
            default -> throw new IllegalArgumentException("Invalid target");
        }
        notifications.save(n);
        audit.transition(n.getTenantId(), n.getId(), old, target.name(), metadata);
    }

    private synchronized void transitionRetry(Notification n, Duration delay, String metadata) {
        String old = n.getStatus().name();
        n.retry(Instant.now().plus(delay));
        notifications.save(n);
        audit.transition(n.getTenantId(), n.getId(), old, n.getStatus().name(), metadata);
    }

    Duration backoff(int attempt) {
        long seconds =
                Math.min(
                        maxBackoff.toSeconds(), initialBackoff.toSeconds() * (1L << (attempt - 1)));
        return Duration.ofSeconds(seconds);
    }

    @PreDestroy
    void shutdownWorkers() {
        workers.shutdown();
    }
}
