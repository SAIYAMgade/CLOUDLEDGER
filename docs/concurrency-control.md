# CloudLedger Concurrency Control & Database Consistency

## 1. Concurrency Problem Scenarios

1. **Simultaneous Double-Withdrawal:** Two concurrent requests attempt to withdraw $7,000 each from an account with only $10,000 available.
2. **Transfer Deadlock:** Transfer 1 moves money from Account A to Account B, while concurrent Transfer 2 moves money from Account B to Account A.
3. **Idempotency Race:** Concurrent HTTP requests arrive simultaneously with the same `Idempotency-Key`.

## 2. Platform Defenses

### A. Ordered Pessimistic Row-Level Locking (`SELECT FOR UPDATE`)
- During funds reservation and ledger journal posting, accounts are acquired with pessimistic row locks.
- To prevent database deadlocks, account locks are **always acquired in sorted order by `account_id ASC`**:
  ```java
  List<UUID> sortedAccountIds = accounts.stream().sorted().toList();
  for (UUID accId : sortedAccountIds) {
      accountRepository.findByIdForUpdate(accId);
  }
  ```

### B. Database Non-Negative Balance Check Constraints
- In addition to application-level checks, PostgreSQL enforces:
  ```sql
  CONSTRAINT chk_positive_balances CHECK (
      (account_type IN ('ASSET', 'EXPENSE') AND available_balance >= 0) OR
      (account_type NOT IN ('ASSET', 'EXPENSE'))
  )
  ```
  Any attempt to force a negative balance throws a database constraint violation error.

### C. Idempotency Unique Key Constraints
- The `transaction_idempotency` table enforces a primary key on `idempotency_key`. Concurrent requests race on the database insert lock; only one succeeds, while competing threads encounter a unique constraint exception and read the committed leader transaction.
