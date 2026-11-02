package com.cloudledger.common.exceptions;

public class AccountFrozenException extends BusinessException {
    public AccountFrozenException(String message) {
        super("ACCOUNT_FROZEN_OR_INACTIVE", message);
    }
}
