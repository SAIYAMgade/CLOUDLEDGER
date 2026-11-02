package com.cloudledger.common.exceptions;

public class InsufficientBalanceException extends BusinessException {
    public InsufficientBalanceException(String message) {
        super("INSUFFICIENT_FUNDS", message);
    }
}
