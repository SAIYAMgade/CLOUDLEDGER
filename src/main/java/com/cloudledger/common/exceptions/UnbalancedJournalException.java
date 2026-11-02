package com.cloudledger.common.exceptions;

public class UnbalancedJournalException extends BusinessException {
    public UnbalancedJournalException(String message) {
        super("UNBALANCED_JOURNAL_ENTRY", message);
    }
}
