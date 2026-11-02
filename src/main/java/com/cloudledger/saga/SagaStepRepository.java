package com.cloudledger.saga;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SagaStepRepository extends JpaRepository<SagaStep, UUID> {
}
