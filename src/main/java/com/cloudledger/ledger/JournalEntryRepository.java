package com.cloudledger.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
    Optional<JournalEntry> findByJournalNumber(String journalNumber);
    Optional<JournalEntry> findByTransactionId(UUID transactionId);
}
