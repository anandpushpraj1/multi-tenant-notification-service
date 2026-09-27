package com.example.notification.delivery;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "delivery_attempts",
        indexes =
                @Index(
                        name = "idx_attempt_notification",
                        columnList = "notification_id,attempt_number"))
public class DeliveryAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryAttemptStatus status;

    @Column(name = "provider_response", columnDefinition = "TEXT")
    private String providerResponse;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant completedAt;

    protected DeliveryAttempt() {}

    public DeliveryAttempt(
            UUID notificationId,
            int attemptNumber,
            DeliveryAttemptStatus status,
            String providerResponse,
            String errorCode,
            String errorMessage,
            Instant startedAt,
            Instant completedAt) {
        this.notificationId = notificationId;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.providerResponse = providerResponse;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public DeliveryAttemptStatus getStatus() {
        return status;
    }

    public String getProviderResponse() {
        return providerResponse;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
