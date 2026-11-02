# CloudLedger Event-Driven Architecture & Transactional Outbox

## 1. Architectural Guarantee

Kafka event publishing is **never** treated as equivalent to financial posting. Financial posting is solely authoritative within PostgreSQL ACID boundaries.

To bridge PostgreSQL state changes with Apache Kafka asynchronously and reliably without two-phase commit (XA), CloudLedger implements the **Transactional Outbox Pattern**:

```mermaid
sequenceDiagram
    participant API as Ingestion API
    participant PG as PostgreSQL
    participant Worker as Outbox Publisher
    participant Kafka as Apache Kafka
    participant Consumer as Idempotent Consumer

    API->>PG: BEGIN TRANSACTION
    API->>PG: INSERT into transactions
    API->>PG: INSERT into outbox_events (Status: PENDING)
    API->>PG: COMMIT
    
    Worker->>PG: SELECT PENDING outbox_events FOR UPDATE SKIP LOCKED
    Worker->>Kafka: Produce message to topic (e.g. transaction.created)
    Worker->>PG: UPDATE outbox_events SET status = 'PUBLISHED'
    
    Kafka->>Consumer: Consume message
    Consumer->>PG: Check consumer_inbox (message_id + consumer_group)
    alt Already processed (duplicate)
        Consumer-->>Kafka: ACK (No-op duplicate execution)
    else First time seen
        Consumer->>PG: Process event & INSERT into consumer_inbox
        Consumer-->>Kafka: ACK
    end
```

## 2. Topic Catalog

- `transaction.created`: Emitted when an ingested transaction is safely stored.
- `transaction.posted`: Emitted upon authoritative ledger journal commit.
- `transaction.failed`: Emitted on permanent failure or terminal validation rejection.
- `transaction.reversed`: Emitted upon successful auditable compensating reversal.
- `ledger.entry.posted`: Emitted when a double-entry journal is posted.
- `transaction.dead-letter`: Bounded retry failures routed for manual investigation.
