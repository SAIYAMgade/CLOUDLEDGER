package com.cloudledger.ledger;

import com.cloudledger.common.MoneyUtils;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_postings")
public class LedgerPosting {

    @Id
    @Column(name = "posting_id")
    private UUID postingId;

    @Column(name = "journal_id", nullable = false)
    private UUID journalId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EntryDirection direction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "running_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal runningBalance;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private Instant postedAt = Instant.now();

    public LedgerPosting() {
        this.postingId = UUID.randomUUID();
    }

    public LedgerPosting(UUID journalId, UUID accountId, EntryDirection direction, BigDecimal amount, String currency, BigDecimal runningBalance) {
        this.postingId = UUID.randomUUID();
        this.journalId = journalId;
        this.accountId = accountId;
        this.direction = direction;
        this.amount = MoneyUtils.format(amount);
        this.currency = currency;
        this.runningBalance = MoneyUtils.format(runningBalance);
        this.postedAt = Instant.now();
    }

    public UUID getPostingId() { return postingId; }
    public void setPostingId(UUID postingId) { this.postingId = postingId; }

    public UUID getJournalId() { return journalId; }
    public void setJournalId(UUID journalId) { this.journalId = journalId; }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public EntryDirection getDirection() { return direction; }
    public void setDirection(EntryDirection direction) { this.direction = direction; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getRunningBalance() { return runningBalance; }
    public void setRunningBalance(BigDecimal runningBalance) { this.runningBalance = runningBalance; }

    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
}
