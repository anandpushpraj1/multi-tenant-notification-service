package com.example.notification.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.notification.audit.AuditEventRepository;
import com.example.notification.channel.ChannelType;
import com.example.notification.delivery.DeliveryAttempt;
import com.example.notification.delivery.DeliveryAttemptRepository;
import com.example.notification.delivery.DeliveryAttemptStatus;
import com.example.notification.template.Template;
import com.example.notification.template.TemplateRepository;
import com.example.notification.tenant.Tenant;
import com.example.notification.tenant.TenantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Testcontainers
@SpringBootTest(properties = "app.dispatcher.interval-ms=3600000")
@AutoConfigureMockMvc
@TestPropertySource(properties = "logging.level.org.springframework.security=ERROR")
class NotificationFlowIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("notifications")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TenantRepository tenantRepository;
    @Autowired TemplateRepository templateRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired DeliveryAttemptRepository attemptRepository;
    @Autowired AuditEventRepository auditRepository;
    @Autowired DispatchEngine dispatchEngine;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PlatformTransactionManager transactionManager;

    private Tenant tenant;
    private Template template;

    @BeforeEach
    void createTenantAndTemplate() {
        String suffix = UUID.randomUUID().toString();
        tenant = tenantRepository.saveAndFlush(new Tenant("integration-tenant-" + suffix, 20));
        template =
                templateRepository.saveAndFlush(
                        new Template(
                                tenant.getId(),
                                "integration-template-" + suffix,
                                ChannelType.EMAIL,
                                "Hello {{name}}",
                                "Hi {{name}}"));
    }

    @Test
    void templateApiRequiresTenantAdmin() throws Exception {
        String body =
                "{\"name\":\"role-test-"
                        + UUID.randomUUID()
                        + "\",\"channel\":\"EMAIL\",\"subject\":\"Subject\",\"body\":\"Body\"}";

        mvc.perform(
                        withTenantHeaders(post("/api/v1/templates"), "PLATFORM_ADMIN")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isForbidden());

        mvc.perform(
                        withTenantHeaders(post("/api/v1/templates"), "TENANT_ADMIN")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void immediateNotificationIsPersistedAndDelivered() throws Exception {
        UUID id = createNotification("customer@example.com", "success-" + UUID.randomUUID(), null);

        assertEquals(
                NotificationStatus.QUEUED,
                notificationRepository.findById(id).orElseThrow().getStatus());

        dispatchEngine.tick();

        awaitStatus(id, NotificationStatus.SENT);
        List<DeliveryAttempt> attempts =
                attemptRepository.findByNotificationIdOrderByAttemptNumberAsc(id);
        assertEquals(1, attempts.size());
        assertEquals(DeliveryAttemptStatus.SUCCESS, attempts.get(0).getStatus());
        assertFalse(auditRepository.findByNotificationIdOrderByCreatedAtAsc(id).isEmpty());
    }

    @Test
    void duplicateIdempotencyKeyReturnsTheSameNotification() throws Exception {
        String key = "idempotency-" + UUID.randomUUID();

        UUID firstId = createNotification("customer@example.com", key, null);
        UUID secondId = createNotification("customer@example.com", key, null);

        assertEquals(firstId, secondId);
        assertTrue(
                notificationRepository
                        .findByTenantIdAndIdempotencyKey(tenant.getId(), key)
                        .isPresent());
    }

    @Test
    void dueScheduledNotificationIsPromotedAndDelivered() throws Exception {
        UUID id =
                createNotification(
                        "customer@example.com",
                        "scheduled-" + UUID.randomUUID(),
                        Instant.now().plusSeconds(3600));
        assertEquals(
                NotificationStatus.SCHEDULED,
                notificationRepository.findById(id).orElseThrow().getStatus());

        jdbcTemplate.update(
                "update notifications set scheduled_at = ?, next_attempt_at = ? where id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(1)),
                java.sql.Timestamp.from(Instant.now().minusSeconds(1)),
                id);
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status -> notificationRepository.promoteScheduled(Instant.now()));

        assertEquals(
                NotificationStatus.QUEUED,
                notificationRepository.findById(id).orElseThrow().getStatus());
        dispatchEngine.tick();
        awaitStatus(id, NotificationStatus.SENT);
    }

    @Test
    void concurrentClaimsOnlyClaimNotificationOnce() throws Exception {
        UUID id = createNotification("customer@example.com", "claim-" + UUID.randomUUID(), null);
        int workerCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        CountDownLatch ready = new CountDownLatch(workerCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Notification>> results = new ArrayList<>();
            for (int worker = 0; worker < workerCount; worker++) {
                results.add(
                        executor.submit(
                                () -> {
                                    ready.countDown();
                                    if (!start.await(5, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException("Start signal timed out");
                                    }
                                    return dispatchEngine.claimOne(tenant.getId());
                                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            List<Notification> claimed = new ArrayList<>();
            for (Future<Notification> result : results) {
                Notification notification = result.get(10, TimeUnit.SECONDS);
                if (notification != null) claimed.add(notification);
            }

            assertEquals(1, claimed.size());
            assertEquals(id, claimed.get(0).getId());
            assertEquals(
                    NotificationStatus.PROCESSING,
                    notificationRepository.findById(id).orElseThrow().getStatus());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void permanentFailureIsRecordedWithoutRetry() throws Exception {
        UUID id =
                createNotification(
                        "permanent-failure@example.com", "permanent-" + UUID.randomUUID(), null);

        dispatchEngine.tick();

        awaitStatus(id, NotificationStatus.FAILED);
        List<DeliveryAttempt> attempts =
                attemptRepository.findByNotificationIdOrderByAttemptNumberAsc(id);
        assertEquals(1, attempts.size());
        assertEquals(DeliveryAttemptStatus.PERMANENT_FAILURE, attempts.get(0).getStatus());
    }

    @Test
    void transientFailuresRetryUntilMaximumAttempts() throws Exception {
        UUID id =
                createNotification(
                        "transient-failure@example.com", "transient-" + UUID.randomUUID(), null);

        for (int attemptNumber = 1; attemptNumber <= 5; attemptNumber++) {
            dispatchEngine.tick();
            awaitAttemptCount(id, attemptNumber);
            NotificationStatus expectedStatus =
                    attemptNumber < 5
                            ? NotificationStatus.RETRY_PENDING
                            : NotificationStatus.FAILED;
            awaitStatus(id, expectedStatus);

            if (attemptNumber < 5) {
                jdbcTemplate.update(
                        "update notifications set next_attempt_at = current_timestamp - interval '1"
                                + " second' where id = ?",
                        id);
            }
        }

        List<DeliveryAttempt> attempts =
                attemptRepository.findByNotificationIdOrderByAttemptNumberAsc(id);
        assertEquals(5, attempts.size());
        assertTrue(
                attempts.stream()
                        .allMatch(a -> a.getStatus() == DeliveryAttemptStatus.TRANSIENT_FAILURE));
    }

    private UUID createNotification(String recipient, String idempotencyKey, Instant scheduledAt)
            throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("templateId", template.getId().toString());
        body.put("recipient", recipient);
        body.put("idempotencyKey", idempotencyKey);
        body.set("variables", objectMapper.createObjectNode().put("name", "Integration"));
        if (scheduledAt != null) body.put("scheduledAt", scheduledAt.toString());

        MvcResult result =
                mvc.perform(
                                withTenantHeaders(post("/api/v1/notifications"), "TENANT_ADMIN")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body.toString()))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.get("id").asText());
    }

    private MockHttpServletRequestBuilder withTenantHeaders(
            MockHttpServletRequestBuilder request, String role) {
        return request.header("X-User-Id", "integration-user")
                .header("X-Tenant-Id", tenant.getId().toString())
                .header("X-Role", role);
    }

    private void awaitStatus(UUID id, NotificationStatus expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Optional<Notification> current = notificationRepository.findById(id);
            if (current.isPresent() && current.get().getStatus() == expected) return;
            Thread.sleep(25);
        }
        Optional<Notification> current = notificationRepository.findById(id);
        int attempts = attemptRepository.findByNotificationIdOrderByAttemptNumberAsc(id).size();
        fail(
                "Notification "
                        + id
                        + " did not reach "
                        + expected
                        + "; actual status="
                        + current.map(Notification::getStatus).orElse(null)
                        + ", attempt count="
                        + attempts);
    }

    private void awaitAttemptCount(UUID id, int expectedCount) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (attemptRepository.findByNotificationIdOrderByAttemptNumberAsc(id).size()
                    >= expectedCount) return;
            Thread.sleep(25);
        }
        fail("Notification " + id + " did not record attempt " + expectedCount);
    }
}
