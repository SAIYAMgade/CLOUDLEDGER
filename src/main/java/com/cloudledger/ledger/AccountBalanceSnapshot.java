package com.cloudledger.ledger;

import com.cloudledger.common.MoneyUtils;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "account_balance_snapshots")
public class AccountBalanceSnapshot {

    @Id
    @Column(name = "snapshot_id")
    private UUID snapshotId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal closingBalance;

    @Column(name = "total_debits", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDebits = MoneyUtils.ZERO;

    @Column(name = "total_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCredits = MoneyUtils.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AccountBalanceSnapshot() {
        this.snapshotId = UUID.randomUUID();
    }

    public UUID getSnapshotId() { return snapshotId; }
    public void setSnapshotId(UUID snapshotId) { this.snapshotId = snapshotId; }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }

    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = MoneyUtils.format(openingBalance); }

    public BigDecimal getClosingBalance() { return closingBalance; }
    public void setClosingBalance(BigDecimal closingBalance) { this.closingBalance = MoneyUtils.format(closingBalance); }

    public BigDecimal getTotalDebits() { return totalDebits; }
    public void setTotalDebits(BigDecimal totalDebits) { this.totalDebits = MoneyUtils.format(totalDebits); }

    public BigDecimal getTotalCredits() { return totalCredits; }
    public void setTotalCredits(BigDecimal totalCredits) { this.totalCredits = MoneyUtils.format(totalCredits); }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
