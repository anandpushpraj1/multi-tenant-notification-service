# Testing Guide

## Prerequisites

- Java 21
- Maven
- Docker running for the PostgreSQL integration tests

The integration suite uses Testcontainers to start PostgreSQL. It does not require a separately installed or configured PostgreSQL server. Run commands from the project root, where `pom.xml` is located.

## Run Tests

Run the complete suite:

```bash
mvn test
```

Run one test class at a time:

```bash
mvn -Dtest=TenantRateLimiterTest test
mvn -Dtest=NotificationServiceTest test
mvn -Dtest=DispatchEngineTest test
mvn -Dtest=TemplateRendererTest test
mvn -Dtest=NotificationFlowIntegrationTest test
```

The unit tests do not require Docker. `NotificationFlowIntegrationTest` does; if Docker is unavailable, Testcontainers cannot start its database and that suite will fail before the tests execute.

## Unit Test Coverage

### `TenantRateLimiterTest`

Covers initial bucket capacity, refill at the configured rate, refill capped at bucket capacity, independent buckets per tenant, rejection of non-positive rates, clearing a tenant bucket, and simultaneous acquisition from multiple threads without exceeding capacity. A controllable clock makes refill checks deterministic.

### `NotificationServiceTest`

Covers immediate notifications starting as `QUEUED`, future notifications starting as `SCHEDULED`, past scheduled times starting as `QUEUED`, duplicate idempotency keys returning the existing notification, rejection of missing template variables, and lookup of templates within the current tenant.

### `DispatchEngineTest`

Covers exponential backoff and its 60-second cap, plus rotation of the starting tenant across dispatcher ticks.

### `TemplateRendererTest`

Covers successful variable substitution and the missing-variable error case.

## PostgreSQL Integration Coverage

`NotificationFlowIntegrationTest` runs the application against a Testcontainers PostgreSQL database. It covers:

- Template API role authorization: a platform admin is rejected and a tenant admin is accepted.
- Immediate notification persistence, successful delivery, delivery-attempt persistence, and audit recording.
- Idempotent notification creation.
- Promotion of a due scheduled notification and its delivery.
- Concurrent database claims: two claimers do not both claim the same notification.
- Permanent provider failure being recorded without a retry.
- Transient provider failures being retried until the five-attempt limit.

The integration test lengthens the automatic dispatcher interval and calls `DispatchEngine.tick()` directly where needed. This makes delivery assertions deterministic while still exercising the real database, repositories, Spring application context, and channel adapters.

## Mock Provider Failure Inputs

The email adapter simulates outcomes based on the recipient string:

- A normal address such as `customer@example.com` simulates success.
- An address containing `permanent-failure` simulates a permanent failure.
- An address containing `transient-failure` simulates a transient failure, which is retried.

These are test/demo behaviors, not real email-provider integrations.
