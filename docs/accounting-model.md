# CloudLedger Double-Entry Accounting Model

## 1. Mathematical Fundamental Invariant

Every posted journal entry in CloudLedger must satisfy the double-entry accounting identity:

$$\sum \text{Debits} \equiv \sum \text{Credits}$$

All calculations are executed in Java using `java.math.BigDecimal` with Banker's Rounding (`RoundingMode.HALF_EVEN`) and stored in PostgreSQL as `NUMERIC(19,4)`. Floating-point types (`float`, `double`) are banned throughout the platform.

## 2. Chart of Accounts & Normal Balances

| Account Type | Normal Balance | Balance Increase | Balance Decrease | Balance Equation |
| :--- | :--- | :--- | :--- | :--- |
| **ASSET** | DEBIT | DEBIT | CREDIT | $\text{Balance} = \sum \text{Debits} - \sum \text{Credits}$ |
| **LIABILITY** | CREDIT | CREDIT | DEBIT | $\text{Balance} = \sum \text{Credits} - \sum \text{Debits}$ |
| **EQUITY** | CREDIT | CREDIT | DEBIT | $\text{Balance} = \sum \text{Credits} - \sum \text{Debits}$ |
| **REVENUE** | CREDIT | CREDIT | DEBIT | $\text{Balance} = \sum \text{Credits} - \sum \text{Debits}$ |
| **EXPENSE** | DEBIT | DEBIT | CREDIT | $\text{Balance} = \sum \text{Debits} - \sum \text{Credits}$ |

## 3. Transfer Accounting Example

**Scenario:** Customer A transfers $5,000 to Customer B.
- Both customer balances represent **Liabilities** on the financial platform's balance sheet (deposits held by the financial institution).

```
Journal Entry Lines:
1. DEBIT:  Customer A Account (Liability) $5,000.0000 (Reduces liability to Customer A)
2. CREDIT: Customer B Account (Liability) $5,000.0000 (Increases liability to Customer B)

Total Debits:  $5,000.0000
Total Credits: $5,000.0000
Net Impact:    $0.0000 (Balanced)
```

## 4. Immutability & Reversals

- Posted journal entries (`POSTED`) are **append-only and immutable**. Updating or deleting a posted line is prohibited at application and database constraint levels.
- Corrections are performed by generating an inverse entry (swapping DEBIT and CREDIT) linked through `reversal_entries`.
