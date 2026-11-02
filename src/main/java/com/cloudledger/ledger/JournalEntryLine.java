package com.cloudledger.ledger;

import com.cloudledger.common.MoneyUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journal_entry_lines")
public class JournalEntryLine {

    @Id
    @Column(name = "line_id")
    private UUID lineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    @JsonIgnore
    private JournalEntry journalEntry;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EntryDirection direction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public JournalEntryLine() {
        this.lineId = UUID.randomUUID();
    }

    public JournalEntryLine(UUID accountId, EntryDirection direction, BigDecimal amount, String currency, int lineOrder) {
        this.lineId = UUID.randomUUID();
        this.accountId = accountId;
        this.direction = direction;
        this.amount = MoneyUtils.format(amount);
        this.currency = currency;
        this.lineOrder = lineOrder;
    }

    public UUID getLineId() { return lineId; }
    public void setLineId(UUID lineId) { this.lineId = lineId; }

    public JournalEntry getJournalEntry() { return journalEntry; }
    public void setJournalEntry(JournalEntry journalEntry) { this.journalEntry = journalEntry; }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public EntryDirection getDirection() { return direction; }
    public void setDirection(EntryDirection direction) { this.direction = direction; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = MoneyUtils.format(amount); }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public int getLineOrder() { return lineOrder; }
    public void setLineOrder(int lineOrder) { this.lineOrder = lineOrder; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
