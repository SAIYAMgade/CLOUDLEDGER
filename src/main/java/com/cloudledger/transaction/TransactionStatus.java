package com.cloudledger.transaction;

public enum TransactionStatus {
    RECEIVED,
    VALIDATING,
    PENDING,
    PROCESSING,
    POSTED,
    FAILED,
    REQUIRES_REVIEW,
    COMPENSATING,
    REVERSED
}
