package com.cloudledger.common.exceptions;

public class IdempotencyConflictException extends BusinessException {
    public IdempotencyConflictException(String message) {
        super("IDEMPOTENCY_KEY_PAYLOAD_MISMATCH", message);
    }
}
