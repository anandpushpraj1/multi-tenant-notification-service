package com.example.notification.audit;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "audit_events",
        indexes = {
            @Index(name = "idx_audit_notification_time", columnList = "notification_id,created_at"),
            @Index(name = "idx_audit_tenant_time", columnList = "tenant_id,created_at")
        })
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "notification_id")
    private UUID notificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditEventType eventType;

    @Column(name = "old_state", length = 30)
    private String oldState;

    @Column(name = "new_state", length = 30)
    private String newState;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false)
    private Instant createdAt;

    protected AuditEvent() {}

    public AuditEvent(
            UUID tenantId,
            UUID notificationId,
            AuditEventType type,
            String oldState,
            String newState,
            String metadata) {
        this.tenantId = tenantId;
        this.notificationId = notificationId;
        this.eventType = type;
        this.oldState = oldState;
        this.newState = newState;
        this.metadata = metadata;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public String getOldState() {
        return oldState;
    }

    public String getNewState() {
        return newState;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
