package com.cloudledger.saga;

public enum SagaStatus {
    STARTED,
    RUNNING,
    COMPLETED,
    COMPENSATING,
    FAILED
}
