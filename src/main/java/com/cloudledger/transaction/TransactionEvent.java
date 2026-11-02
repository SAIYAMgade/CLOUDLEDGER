package com.cloudledger.transaction;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_events")
public class TransactionEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TransactionEvent() {
        this.eventId = UUID.randomUUID();
    }

    public TransactionEvent(UUID transactionId, String eventType, String payload) {
        this.eventId = UUID.randomUUID();
        this.transactionId = transactionId;
        this.eventType = eventType;
        this.payload = payload;
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
