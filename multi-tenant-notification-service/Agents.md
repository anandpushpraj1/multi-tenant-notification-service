# Development Agents / AI Workflow

This file records the development workflow used for the assignment.

## Approach

- Requirement extraction from the supplied assignment PDF.
- Architecture-first design before implementation.
- Modular-monolith decision based on the explicit out-of-scope distributed-systems constraint.
- Incremental implementation by domain: persistence, APIs, dispatch, fairness/rate limiting, retry/idempotency, tests.
- AI assistance was used for scaffolding, review, test-case brainstorming, and documentation; design decisions were manually reviewed against the assignment scope.

## Review checklist

- Does every tenant-owned resource enforce tenant isolation?
- Are state transitions persisted?
- Are retry attempts persisted?
- Is dispatch concurrency bounded?
- Can one tenant starve another?
- Is per-tenant rate limiting enforced?
- Is notification creation idempotent?
- Are scheduled and immediate paths converging on the same dispatcher?
