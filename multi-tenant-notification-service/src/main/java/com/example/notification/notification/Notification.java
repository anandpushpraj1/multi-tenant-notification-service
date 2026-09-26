package com.example.notification.notification;

import com.example.notification.channel.ChannelType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="notifications", indexes={
    @Index(name="idx_notification_status_next", columnList="status,next_attempt_at"),
    @Index(name="idx_notification_status_scheduled", columnList="status,scheduled_at"),
    @Index(name="idx_notification_tenant_status", columnList="tenant_id,status"),
    @Index(name="idx_notification_tenant_created", columnList="tenant_id,created_at")
}, uniqueConstraints=@UniqueConstraint(name="uk_notification_idempotency", columnNames={"tenant_id","idempotency_key"}))
public class Notification {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="tenant_id", nullable=false) private UUID tenantId;
    @Column(name="template_id", nullable=false) private UUID templateId;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private ChannelType channel;
    @Column(nullable=false, length=320) private String recipient;
    @Column(nullable=false, columnDefinition="TEXT") private String variablesJson;
    @Column(nullable=false, length=100) private String idempotencyKey;
    @Column(nullable=false, length=30) @Enumerated(EnumType.STRING) private NotificationStatus status;
    @Column(name="scheduled_at") private Instant scheduledAt;
    @Column(name="next_attempt_at") private Instant nextAttemptAt;
    @Column(name="attempt_count", nullable=false) private int attemptCount;
    @Version private long version;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    protected Notification(){}
    public Notification(UUID tenantId,UUID templateId,ChannelType channel,String recipient,String variablesJson,String idempotencyKey,Instant scheduledAt){
        this.tenantId=tenantId;this.templateId=templateId;this.channel=channel;this.recipient=recipient;this.variablesJson=variablesJson;this.idempotencyKey=idempotencyKey;this.scheduledAt=scheduledAt;this.status=scheduledAt!=null && scheduledAt.isAfter(Instant.now())?NotificationStatus.SCHEDULED:NotificationStatus.QUEUED;this.nextAttemptAt=scheduledAt;}
    @PrePersist void p(){createdAt=updatedAt=Instant.now(); if(nextAttemptAt==null) nextAttemptAt=createdAt;}
    @PreUpdate void u(){updatedAt=Instant.now();}
    public UUID getId(){return id;} public UUID getTenantId(){return tenantId;} public UUID getTemplateId(){return templateId;} public ChannelType getChannel(){return channel;} public String getRecipient(){return recipient;}
    public String getVariablesJson(){return variablesJson;} public String getIdempotencyKey(){return idempotencyKey;} public NotificationStatus getStatus(){return status;} public Instant getScheduledAt(){return scheduledAt;} public Instant getNextAttemptAt(){return nextAttemptAt;} public int getAttemptCount(){return attemptCount;}
    public void queue(){status=NotificationStatus.QUEUED; nextAttemptAt=Instant.now();}
    public void processing(){status=NotificationStatus.PROCESSING;}
    public void sent(){status=NotificationStatus.SENT;nextAttemptAt=null;}
    public void retry(Instant at){status=NotificationStatus.RETRY_PENDING;nextAttemptAt=at;}
    public void failed(){status=NotificationStatus.FAILED;nextAttemptAt=null;}
    public void cancelled(){status=NotificationStatus.CANCELLED;nextAttemptAt=null;}
    public int incrementAttempt(){return ++attemptCount;}
}
