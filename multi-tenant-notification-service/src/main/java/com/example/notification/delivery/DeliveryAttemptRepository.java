package com.example.notification.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {
    List<DeliveryAttempt> findByNotificationIdOrderByAttemptNumberAsc(UUID notificationId);
}
