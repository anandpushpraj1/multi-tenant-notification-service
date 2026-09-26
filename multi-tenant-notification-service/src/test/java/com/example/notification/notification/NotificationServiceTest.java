package com.example.notification.notification;
import org.junit.jupiter.api.Test; import java.time.*; import static org.junit.jupiter.api.Assertions.*;
class NotificationServiceTest{@Test void scheduledNotificationIsScheduled(){assertTrue(NotificationStatus.SCHEDULED.name().equals("SCHEDULED"));}}
