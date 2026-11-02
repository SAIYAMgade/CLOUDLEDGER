package com.cloudledger.ledger.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class GeneralLedgerDTO {
    private UUID accountId;
    private String accountNumber;
    private String accountName;
    private String accountType;
    private String currency;
    private BigDecimal currentBalance;
    private List<LedgerRecordDTO> records;

    public GeneralLedgerDTO() {}

    public static class LedgerRecordDTO {
        private UUID postingId;
        private UUID journalId;
        private Instant postedAt;
        private String direction;
        private BigDecimal amount;
        private BigDecimal runningBalance;

        public LedgerRecordDTO() {}

        public LedgerRecordDTO(UUID postingId, UUID journalId, Instant postedAt, String direction, BigDecimal amount, BigDecimal runningBalance) {
            this.postingId = postingId;
            this.journalId = journalId;
            this.postedAt = postedAt;
            this.direction = direction;
            this.amount = amount;
            this.runningBalance = runningBalance;
        }

        public UUID getPostingId() { return postingId; }
        public void setPostingId(UUID postingId) { this.postingId = postingId; }

        public UUID getJournalId() { return journalId; }
        public void setJournalId(UUID journalId) { this.journalId = journalId; }

        public Instant getPostedAt() { return postedAt; }
        public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }

        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public BigDecimal getRunningBalance() { return runningBalance; }
        public void setRunningBalance(BigDecimal runningBalance) { this.runningBalance = runningBalance; }
    }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public List<LedgerRecordDTO> getRecords() { return records; }
    public void setRecords(List<LedgerRecordDTO> records) { this.records = records; }
}
