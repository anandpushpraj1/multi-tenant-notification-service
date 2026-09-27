package com.example.notification.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByNotificationIdOrderByCreatedAtAsc(UUID notificationId);
}
