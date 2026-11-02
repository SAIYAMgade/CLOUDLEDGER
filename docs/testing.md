# CloudLedger Automated Testing Strategy

## 1. Test Invariants & Scenarios Covered

The automated test suite explicitly tests core financial and distributed systems invariants:

1. **Double-Entry Balance Invariant (`LedgerEngineInvariantsTest`):**
   - Asserts that when $\sum \text{Debit} \neq \sum \text{Credit}$, `UnbalancedJournalException` is thrown.
   - Verifies that posted entries update running balances and persist outbox events.
2. **Idempotency Replay & Conflict (`IdempotencyEngineTest`):**
   - Matching key + matching payload -> Idempotent replay permitted.
   - Matching key + modified payload -> `IdempotencyConflictException` (HTTP 409).
3. **Concurrency Race Condition & Negative Balance Violation (`ConcurrentAccountBalanceTest`):**
   - Simultaneous double withdrawals ($7,000 + $7,000 on a $10,000 balance).
   - Verifies that only the first withdrawal succeeds, the second throws `InsufficientBalanceException`, and the final balance remains strictly non-negative ($3,000).
4. **Saga Orchestrator & Failure Compensation (`TransferSagaCoordinatorTest`):**
   - Verifies end-to-end multi-step saga state progression.
   - Tests failure handling and compensation execution without leaving dirty state.
5. **Cryptographic Audit Chain Integrity (`AuditHashChainTest`):**
   - Validates multi-block SHA-256 hash chaining.
   - Asserts that tampered records are flagged with exact block index.

## 2. Running Automated Tests

```bash
# Run all backend JUnit 5 invariant tests
mvn test

# Run frontend production build & type checks
cd frontend && npm run build
```
