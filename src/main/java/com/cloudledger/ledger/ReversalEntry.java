package com.cloudledger.ledger;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reversal_entries")
public class ReversalEntry {

    @Id
    @Column(name = "reversal_id")
    private UUID reversalId;

    @Column(name = "original_journal_id", nullable = false, unique = true)
    private UUID originalJournalId;

    @Column(name = "reversal_journal_id", nullable = false, unique = true)
    private UUID reversalJournalId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "reversed_by", nullable = false, length = 64)
    private String reversedBy;

    @Column(name = "reversed_at", nullable = false, updatable = false)
    private Instant reversedAt = Instant.now();

    public ReversalEntry() {
        this.reversalId = UUID.randomUUID();
    }

    public ReversalEntry(UUID originalJournalId, UUID reversalJournalId, String reason, String reversedBy) {
        this.reversalId = UUID.randomUUID();
        this.originalJournalId = originalJournalId;
        this.reversalJournalId = reversalJournalId;
        this.reason = reason;
        this.reversedBy = reversedBy;
        this.reversedAt = Instant.now();
    }

    public UUID getReversalId() { return reversalId; }
    public void setReversalId(UUID reversalId) { this.reversalId = reversalId; }

    public UUID getOriginalJournalId() { return originalJournalId; }
    public void setOriginalJournalId(UUID originalJournalId) { this.originalJournalId = originalJournalId; }

    public UUID getReversalJournalId() { return reversalJournalId; }
    public void setReversalJournalId(UUID reversalJournalId) { this.reversalJournalId = reversalJournalId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getReversedBy() { return reversedBy; }
    public void setReversedBy(String reversedBy) { this.reversedBy = reversedBy; }

    public Instant getReversedAt() { return reversedAt; }
    public void setReversedAt(Instant reversedAt) { this.reversedAt = reversedAt; }
}
