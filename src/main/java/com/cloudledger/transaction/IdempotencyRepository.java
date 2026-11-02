package com.cloudledger.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<TransactionIdempotency, String> {
    Optional<TransactionIdempotency> findByIdempotencyKey(String idempotencyKey);
}
