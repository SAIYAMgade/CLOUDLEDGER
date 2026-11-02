package com.cloudledger.ledger;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    @Id
    @Column(name = "journal_id")
    private UUID journalId;

    @Column(name = "transaction_id", unique = true)
    private UUID transactionId;

    @Column(name = "journal_number", nullable = false, unique = true, length = 64)
    private String journalNumber;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JournalStatus status = JournalStatus.DRAFT;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("lineOrder ASC")
    private List<JournalEntryLine> lines = new ArrayList<>();

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    public JournalEntry() {
        this.journalId = UUID.randomUUID();
    }

    public void addLine(JournalEntryLine line) {
        lines.add(line);
        line.setJournalEntry(this);
    }

    public UUID getJournalId() { return journalId; }
    public void setJournalId(UUID journalId) { this.journalId = journalId; }

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public String getJournalNumber() { return journalNumber; }
    public void setJournalNumber(String journalNumber) { this.journalNumber = journalNumber; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public JournalStatus getStatus() { return status; }
    public void setStatus(JournalStatus status) { this.status = status; }

    public List<JournalEntryLine> getLines() { return lines; }
    public void setLines(List<JournalEntryLine> lines) { this.lines = lines; }

    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
