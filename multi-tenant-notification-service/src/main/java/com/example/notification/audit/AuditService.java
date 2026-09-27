package com.example.notification.audit;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuditService {
    private final AuditEventRepository repo;

    public AuditService(AuditEventRepository repo) {
        this.repo = repo;
    }

    public void transition(
            UUID tenant, UUID notification, String oldS, String newS, String metadata) {
        repo.save(
                new AuditEvent(
                        tenant,
                        notification,
                        AuditEventType.STATE_TRANSITION,
                        oldS,
                        newS,
                        metadata));
    }

    public void event(UUID tenant, UUID notification, AuditEventType type, String metadata) {
        repo.save(new AuditEvent(tenant, notification, type, null, null, metadata));
    }

    public List<AuditEvent> find(UUID notification) {
        return repo.findByNotificationIdOrderByCreatedAtAsc(notification);
    }
}
