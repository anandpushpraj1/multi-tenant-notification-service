# Multi-Tenant Notification Service

Spring Boot 3.4 / Java 21 implementation of the supplied 48-hour assignment.

## Scope decisions

The assignment explicitly expects REST APIs, database persistence, basic RBAC, validation/error handling, and unit/integration tests. It explicitly excludes UI, deployment/containerization/CI/CD, distributed systems/microservices, advanced authentication, and production-grade observability.

Therefore this implementation uses a **modular monolith** with PostgreSQL and a database-backed asynchronous dispatch pipeline. Kafka/RabbitMQ/Redis/Kubernetes are deliberately not used.

## Architecture

1. REST API validates and persists a notification.
2. Immediate notifications enter `QUEUED`; future notifications enter `SCHEDULED`.
3. A scheduler promotes due scheduled notifications to `QUEUED`.
4. A dispatcher discovers eligible tenants in round-robin order.
5. A per-tenant token bucket enforces the configured rate limit.
6. PostgreSQL row locking with `FOR UPDATE` semantics prevents two workers from claiming the same notification.
7. A bounded executor dispatches concurrently through a channel abstraction.
8. Transient failures are retried with exponential backoff; permanent failures are terminal.
9. Every delivery attempt and state transition is persisted.

## Important assumptions

- Single application instance for this assignment; process-local rate limiting and fairness are therefore intentional.
- Channel providers are mock adapters. Recipient strings containing `transient-failure` or `permanent-failure` are useful for demos/tests.
- Exactly-once delivery across an external provider boundary cannot be guaranteed unless the provider honors the supplied idempotency key. The service guarantees idempotent notification creation and durable state transitions within its own database boundary.
- Shared PostgreSQL schema with `tenant_id` provides tenant isolation.
- Header-based authentication is intentionally simple because OAuth/SSO/MFA are out of scope. Use `X-User-Id`, `X-Tenant-Id`, and `X-Role`. Tenant-owned APIs require `TENANT_ADMIN`; tenant administration APIs require `PLATFORM_ADMIN`.

## API examples

Create a template:

```http
POST /api/v1/templates
X-User-Id: tenant-admin
X-Tenant-Id: 00000000-0000-0000-0000-000000000001
X-Role: TENANT_ADMIN
Content-Type: application/json

{"name":"order-shipped","channel":"EMAIL","subject":"Order {{orderId}} shipped","body":"Hello {{name}}, your order {{orderId}} is shipped."}
```

Create an immediate notification:

```http
POST /api/v1/notifications
X-User-Id: tenant-admin
X-Tenant-Id: 00000000-0000-0000-0000-000000000001
X-Role: TENANT_ADMIN
Content-Type: application/json

{"templateId":"<template-id>","recipient":"customer@example.com","variables":{"name":"Anand","orderId":"ORD-123"},"idempotencyKey":"order-ORD-123-shipped"}
```

Schedule a notification by adding `scheduledAt` in ISO-8601 format.

## Running locally

Requires Java 21 and PostgreSQL.

```bash
createdb notifications
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
mvn spring-boot:run
```

## Tests

```bash
mvn test
```

The project includes unit tests for template rendering/rate limiting plus a PostgreSQL Testcontainers integration test covering the REST/security path. The container-based integration test requires Docker.

## Trade-offs

### PostgreSQL queue vs Kafka
PostgreSQL keeps persistence, scheduling, retries and audit history in one system and fits the assignment's non-distributed constraint. Kafka would be a better production choice at very large scale but would add infrastructure and distributed-system complexity that the brief explicitly excludes.

### In-memory rate limiting vs Redis
In-memory token buckets are enough for one instance. A shared limiter would be required when horizontally scaling.

### Round-robin fairness vs weighted fair queue
Round-robin is deterministic and easy to reason about. Weighted fairness can be introduced later if tenants receive different priority classes.

### Mock providers vs real integrations
Mock adapters keep the assignment self-contained and testable. Real provider credentials/configuration are outside the supplied scope.

## Submission artifacts

- `README.md`
- `Agents.md`
- `skills/` development notes
- source code and tests
- `docs/architecture.md`
- `docs/video-script.md`
