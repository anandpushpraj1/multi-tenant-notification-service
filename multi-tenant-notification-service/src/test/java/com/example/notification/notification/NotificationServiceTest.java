package com.example.notification.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.notification.audit.AuditService;
import com.example.notification.channel.ChannelType;
import com.example.notification.common.ApiException;
import com.example.notification.security.CurrentUser;
import com.example.notification.security.SecurityUtils;
import com.example.notification.template.Template;
import com.example.notification.template.TemplateRenderer;
import com.example.notification.template.TemplateRepository;
import com.example.notification.tenant.Role;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.*;
import java.util.*;

class NotificationServiceTest {
    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final TemplateRepository templates = mock(TemplateRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final UUID tenantId = UUID.randomUUID();
    private final UUID templateId = UUID.randomUUID();
    private final Template template =
            new Template(tenantId, "welcome", ChannelType.EMAIL, "Hello {{name}}", "Hi {{name}}");
    private final NotificationService service =
            new NotificationService(
                    repo, templates, new TemplateRenderer(), new ObjectMapper(), audit);

    @BeforeEach
    void setTenantUser() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new CurrentUser("tenant-admin", tenantId, Role.TENANT_ADMIN),
                                null,
                                List.of()));
        when(templates.findByIdAndTenantId(templateId, tenantId)).thenReturn(Optional.of(template));
        when(repo.findByTenantIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(repo.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void immediateNotificationStartsQueuedAndIsAudited() {
        Notification notification =
                service.create(
                        templateId,
                        "customer@example.com",
                        Map.of("name", "Asha"),
                        null,
                        "immediate-1");

        assertEquals(NotificationStatus.QUEUED, notification.getStatus());
        assertEquals(tenantId, notification.getTenantId());
        verify(repo).save(notification);
        verify(audit)
                .event(
                        tenantId,
                        notification.getId(),
                        com.example.notification.audit.AuditEventType.CREATED,
                        "status=QUEUED");
    }

    @Test
    void futureNotificationStartsScheduled() {
        Instant scheduledAt = Instant.now().plusSeconds(60);

        Notification notification =
                service.create(
                        templateId,
                        "customer@example.com",
                        Map.of("name", "Asha"),
                        scheduledAt,
                        "scheduled-1");

        assertEquals(NotificationStatus.SCHEDULED, notification.getStatus());
        assertEquals(scheduledAt, notification.getScheduledAt());
    }

    @Test
    void pastScheduledTimeStartsQueued() {
        Notification notification =
                service.create(
                        templateId,
                        "customer@example.com",
                        Map.of("name", "Asha"),
                        Instant.now().minusSeconds(1),
                        "past-schedule-1");

        assertEquals(NotificationStatus.QUEUED, notification.getStatus());
    }

    @Test
    void duplicateIdempotencyKeyReturnsExistingNotification() {
        Notification existing =
                new Notification(
                        tenantId,
                        templateId,
                        ChannelType.EMAIL,
                        "customer@example.com",
                        "{\"name\":\"Asha\"}",
                        "duplicate-1",
                        null);
        when(repo.findByTenantIdAndIdempotencyKey(tenantId, "duplicate-1"))
                .thenReturn(Optional.of(existing));

        Notification result =
                service.create(
                        templateId,
                        "customer@example.com",
                        Map.of("name", "Asha"),
                        null,
                        "duplicate-1");

        assertSame(existing, result);
        verify(repo, never()).save(any(Notification.class));
        verifyNoInteractions(audit);
    }

    @Test
    void missingTemplateVariableIsRejectedBeforeSave() {
        assertThrows(
                ApiException.class,
                () ->
                        service.create(
                                templateId, "customer@example.com", Map.of(), null, "invalid-1"));

        verify(repo, never()).save(any(Notification.class));
    }

    @Test
    void templateMustBelongToCurrentTenant() {
        when(templates.findByIdAndTenantId(templateId, tenantId)).thenReturn(Optional.empty());

        assertThrows(
                ApiException.class,
                () ->
                        service.create(
                                templateId,
                                "customer@example.com",
                                Map.of("name", "Asha"),
                                null,
                                "wrong-tenant-1"));

        verify(repo, never()).save(any(Notification.class));
        assertEquals(tenantId, SecurityUtils.tenant());
    }
}
