package com.cloudledger.account;

public enum AccountType {
    ASSET,
    LIABILITY,
    EQUITY,
    REVENUE,
    EXPENSE;

    public boolean isNormalDebit() {
        return this == ASSET || this == EXPENSE;
    }

    public boolean isNormalCredit() {
        return this == LIABILITY || this == EQUITY || this == REVENUE;
    }
}
