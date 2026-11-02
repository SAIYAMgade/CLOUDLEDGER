package com.cloudledger.ledger.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TrialBalanceDTO {
    private Instant asOfDate;
    private String currency;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private boolean isBalanced;
    private List<TrialBalanceItem> items;

    public TrialBalanceDTO() {}

    public static class TrialBalanceItem {
        private UUID accountId;
        private String accountNumber;
        private String accountName;
        private String accountType;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;

        public TrialBalanceItem() {}

        public TrialBalanceItem(UUID accountId, String accountNumber, String accountName, String accountType, BigDecimal debitAmount, BigDecimal creditAmount) {
            this.accountId = accountId;
            this.accountNumber = accountNumber;
            this.accountName = accountName;
            this.accountType = accountType;
            this.debitAmount = debitAmount;
            this.creditAmount = creditAmount;
        }

        public UUID getAccountId() { return accountId; }
        public void setAccountId(UUID accountId) { this.accountId = accountId; }

        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }

        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }

        public BigDecimal getDebitAmount() { return debitAmount; }
        public void setDebitAmount(BigDecimal debitAmount) { this.debitAmount = debitAmount; }

        public BigDecimal getCreditAmount() { return creditAmount; }
        public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount = creditAmount; }
    }

    public Instant getAsOfDate() { return asOfDate; }
    public void setAsOfDate(Instant asOfDate) { this.asOfDate = asOfDate; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getTotalDebits() { return totalDebits; }
    public void setTotalDebits(BigDecimal totalDebits) { this.totalDebits = totalDebits; }

    public BigDecimal getTotalCredits() { return totalCredits; }
    public void setTotalCredits(BigDecimal totalCredits) { this.totalCredits = totalCredits; }

    public boolean isBalanced() { return isBalanced; }
    public void setBalanced(boolean balanced) { isBalanced = balanced; }

    public List<TrialBalanceItem> getItems() { return items; }
    public void setItems(List<TrialBalanceItem> items) { this.items = items; }
}
