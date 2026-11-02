package com.cloudledger.ledger.dto;

import com.cloudledger.ledger.EntryDirection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class JournalLineDTO {
    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotNull(message = "Entry direction is required (DEBIT or CREDIT)")
    private EntryDirection direction;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    public JournalLineDTO() {}

    public JournalLineDTO(UUID accountId, EntryDirection direction, BigDecimal amount, String currency) {
        this.accountId = accountId;
        this.direction = direction;
        this.amount = amount;
        this.currency = currency;
    }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public EntryDirection getDirection() { return direction; }
    public void setDirection(EntryDirection direction) { this.direction = direction; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
