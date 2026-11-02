package com.cloudledger.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountBalanceSnapshotRepository extends JpaRepository<AccountBalanceSnapshot, UUID> {
    Optional<AccountBalanceSnapshot> findByAccountIdAndSnapshotDate(UUID accountId, LocalDate snapshotDate);
}
