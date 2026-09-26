# Architecture Notes

## State machine

```text
SCHEDULED -> QUEUED -> PROCESSING -> SENT
                         |
                         +-> RETRY_PENDING -> PROCESSING
                         |
                         +-> FAILED
```

`CANCELLED` is allowed from `SCHEDULED`, `QUEUED`, and `RETRY_PENDING`.

## Fairness

Each dispatcher tick obtains the set of tenants with eligible work and starts at a rotating cursor. At most one notification per tenant is claimed per tick. This prevents a large tenant queue from monopolizing the worker pool in the single-instance scope.

## Database claim

The repository uses a pessimistic lock when selecting a candidate notification. The selected row is immediately moved to `PROCESSING` in a transaction, preventing duplicate claims by concurrent dispatcher invocations.

## Retry

Transient failures use exponential backoff: 1s, 2s, 4s, 8s, ... capped at 60s, with a maximum of five attempts.
