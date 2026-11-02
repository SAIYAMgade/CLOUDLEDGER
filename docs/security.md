# CloudLedger Security Engineering Specification

## 1. Authentication & Role-Based Access Control (RBAC)

CloudLedger enforces stateless JWT authentication with HMAC-SHA256 signatures, alongside database-driven Role-Based Access Control:

| Role | Permissions |
| :--- | :--- |
| **`ROLE_ADMINISTRATOR`** | User management, system configuration limits, account freeze/unfreeze, system diagnostics. |
| **`ROLE_FINANCE_OPERATOR`** | Submit transactions, create financial accounts, query account balances and statements. |
| **`ROLE_FINANCE_APPROVER`** | Authorize high-value exceptions, reverse posted transactions, review flagged operations. |
| **`ROLE_AUDITOR`** | Read-only access to ledger journals, trial balance, general ledger, and cryptographic audit logs. |

## 2. Tamper-Evident SHA-256 Audit Chain

All critical business and administrative actions are logged to `audit_logs` using a cryptographic block-linking design:

$$\text{curr\_hash} = \text{SHA-256}(\text{prev\_hash} : \text{user\_id} : \text{action} : \text{entity\_type} : \text{entity\_id} : \text{details})$$

- If any row in the audit table is modified, deleted, or backdated, calling `GET /api/v1/audit-logs/verify` detects the broken link and returns the exact corrupted record identifier.
