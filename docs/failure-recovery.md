# CloudLedger Failure Recovery & Saga Orchestration

## 1. Saga Orchestration Architecture

CloudLedger utilizes an **Orchestrated Saga Pattern** to coordinate multi-step financial transaction operations across account validation, balance reservations, double-entry journal postings, and final ledger commits.

State persistence in `saga_instances` and `saga_steps` guarantees that if a node crashes mid-workflow, the recovery coordinator can resume from the last completed checkpoint.

## 2. Failure Recovery Scenarios

| Failure Point | System Action | Compensating Transaction |
| :--- | :--- | :--- |
| **Insufficient balance during reservation** | Abort workflow immediately. Mark transaction `FAILED`. | None required (no funds were reserved). |
| **Ledger posting failure after fund reservation** | Trigger compensation step `COMPENSATE_RELEASE_RESERVED_FUNDS`. | Releases reserved balance back to source account. |
| **Network timeout between Gateway and Service** | Client retries with same `Idempotency-Key`. | Returns existing transaction outcome without duplicate posting. |
| **Kafka broker crash during outbox dispatch** | Outbox events remain in `PENDING` status. | Poller retries upon broker reconnection with exponential backoff. |
| **Service crash during journal posting** | PostgreSQL rolls back active uncommitted transaction. | Unfinished saga picked up by recovery poller based on `updated_at` threshold. |
