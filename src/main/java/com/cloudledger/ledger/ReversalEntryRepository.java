package com.cloudledger.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReversalEntryRepository extends JpaRepository<ReversalEntry, UUID> {
    Optional<ReversalEntry> findByOriginalJournalId(UUID originalJournalId);
}
