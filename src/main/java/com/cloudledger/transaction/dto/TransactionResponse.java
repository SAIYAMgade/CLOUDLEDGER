package com.cloudledger.transaction.dto;

import com.cloudledger.transaction.Transaction;
import com.cloudledger.transaction.TransactionStatus;
import com.cloudledger.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransactionResponse {
    private UUID transactionId;
    private String externalReference;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private TransactionType transactionType;
    private TransactionStatus status;
    private String idempotencyKey;
    private String description;
    private String errorCode;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private boolean idempotentReplay = false;

    public TransactionResponse() {}

    public static TransactionResponse from(Transaction tx) {
        TransactionResponse r = new TransactionResponse();
        r.setTransactionId(tx.getTransactionId());
        r.setExternalReference(tx.getExternalReference());
        r.setSourceAccountId(tx.getSourceAccountId());
        r.setDestinationAccountId(tx.getDestinationAccountId());
        r.setAmount(tx.getAmount());
        r.setCurrency(tx.getCurrency());
        r.setTransactionType(tx.getTransactionType());
        r.setStatus(tx.getStatus());
        r.setIdempotencyKey(tx.getIdempotencyKey());
        r.setDescription(tx.getDescription());
        r.setErrorCode(tx.getErrorCode());
        r.setErrorMessage(tx.getErrorMessage());
        r.setCreatedAt(tx.getCreatedAt());
        r.setUpdatedAt(tx.getUpdatedAt());
        r.setCompletedAt(tx.getCompletedAt());
        return r;
    }

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }

    public UUID getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(UUID sourceAccountId) { this.sourceAccountId = sourceAccountId; }

    public UUID getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(UUID destinationAccountId) { this.destinationAccountId = destinationAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public boolean isIdempotentReplay() { return idempotentReplay; }
    public void setIdempotentReplay(boolean idempotentReplay) { this.idempotentReplay = idempotentReplay; }
}
