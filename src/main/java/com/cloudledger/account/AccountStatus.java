package com.cloudledger.account;

public enum AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED,
    PENDING_VERIFICATION;

    public boolean canPost() {
        return this == ACTIVE;
    }
}
