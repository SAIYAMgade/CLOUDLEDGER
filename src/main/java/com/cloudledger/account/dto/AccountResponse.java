package com.cloudledger.account.dto;

import com.cloudledger.account.Account;
import com.cloudledger.account.AccountStatus;
import com.cloudledger.account.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AccountResponse {
    private UUID accountId;
    private String accountNumber;
    private String accountName;
    private String customerReference;
    private AccountType accountType;
    private String currency;
    private AccountStatus status;
    private BigDecimal availableBalance;
    private BigDecimal ledgerBalance;
    private Instant createdAt;
    private Instant updatedAt;

    public AccountResponse() {}

    public static AccountResponse from(Account a) {
        AccountResponse res = new AccountResponse();
        res.setAccountId(a.getAccountId());
        res.setAccountNumber(a.getAccountNumber());
        res.setAccountName(a.getAccountName());
        res.setCustomerReference(a.getCustomerReference());
        res.setAccountType(a.getAccountType());
        res.setCurrency(a.getCurrency());
        res.setStatus(a.getStatus());
        res.setAvailableBalance(a.getAvailableBalance());
        res.setLedgerBalance(a.getLedgerBalance());
        res.setCreatedAt(a.getCreatedAt());
        res.setUpdatedAt(a.getUpdatedAt());
        return res;
    }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getCustomerReference() { return customerReference; }
    public void setCustomerReference(String customerReference) { this.customerReference = customerReference; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }

    public BigDecimal getLedgerBalance() { return ledgerBalance; }
    public void setLedgerBalance(BigDecimal ledgerBalance) { this.ledgerBalance = ledgerBalance; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
