package com.cloudledger.account.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AccountStatementDTO {
    private UUID accountId;
    private String accountNumber;
    private String accountName;
    private String currency;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private List<StatementLineDTO> lines;

    public AccountStatementDTO() {}

    public static class StatementLineDTO {
        private UUID entryId;
        private Instant date;
        private String description;
        private String direction;
        private BigDecimal amount;
        private BigDecimal runningBalance;

        public StatementLineDTO() {}

        public StatementLineDTO(UUID entryId, Instant date, String description, String direction, BigDecimal amount, BigDecimal runningBalance) {
            this.entryId = entryId;
            this.date = date;
            this.description = description;
            this.direction = direction;
            this.amount = amount;
            this.runningBalance = runningBalance;
        }

        public UUID getEntryId() { return entryId; }
        public void setEntryId(UUID entryId) { this.entryId = entryId; }

        public Instant getDate() { return date; }
        public void setDate(Instant date) { this.date = date; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

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

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }

    public BigDecimal getClosingBalance() { return closingBalance; }
    public void setClosingBalance(BigDecimal closingBalance) { this.closingBalance = closingBalance; }

    public BigDecimal getTotalDebits() { return totalDebits; }
    public void setTotalDebits(BigDecimal totalDebits) { this.totalDebits = totalDebits; }

    public BigDecimal getTotalCredits() { return totalCredits; }
    public void setTotalCredits(BigDecimal totalCredits) { this.totalCredits = totalCredits; }

    public List<StatementLineDTO> getLines() { return lines; }
    public void setLines(List<StatementLineDTO> lines) { this.lines = lines; }
}
