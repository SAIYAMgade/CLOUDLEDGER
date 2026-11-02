-- V2__seed_initial_data.sql: Baseline Reference Data for Roles, Users & Initial Accounts

-- 1. Insert Core Roles
INSERT INTO roles (role_id, description) VALUES
('ROLE_ADMINISTRATOR', 'System administrator with full configuration and user management permissions'),
('ROLE_FINANCE_OPERATOR', 'Finance operator capable of submitting and tracking transactions'),
('ROLE_FINANCE_APPROVER', 'Approver for high-value transactions and exceptions'),
('ROLE_AUDITOR', 'Read-only auditor with access to ledger and compliance trails');

-- 2. Insert Users (Default password: 'password123' hashed with BCrypt)
-- Hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (user_id, username, email, password_hash, status) VALUES
('11111111-1111-1111-1111-111111111111', 'admin', 'admin@cloudledger.internal', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE'),
('22222222-2222-2222-2222-222222222222', 'operator', 'operator@cloudledger.internal', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE'),
('33333333-3333-3333-3333-333333333333', 'approver', 'approver@cloudledger.internal', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE'),
('44444444-4444-4444-4444-444444444444', 'auditor', 'auditor@cloudledger.internal', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE');

-- 3. Assign User Roles
INSERT INTO user_roles (user_id, role_id) VALUES
('11111111-1111-1111-1111-111111111111', 'ROLE_ADMINISTRATOR'),
('22222222-2222-2222-2222-222222222222', 'ROLE_FINANCE_OPERATOR'),
('33333333-3333-3333-3333-333333333333', 'ROLE_FINANCE_APPROVER'),
('44444444-4444-4444-4444-444444444444', 'ROLE_AUDITOR');

-- 4. Initial Accounts for Standard Financial Operations
INSERT INTO accounts (account_id, account_number, account_name, customer_reference, account_type, currency, status, available_balance, ledger_balance, version) VALUES
('a0000000-0000-0000-0000-000000000001', 'AC-CASH-SETTLE-01', 'Central Bank Settlement Account', 'SYS-CASH-01', 'ASSET', 'USD', 'ACTIVE', 10000000.0000, 10000000.0000, 0),
('a0000000-0000-0000-0000-000000000002', 'AC-CUST-CORP-A01', 'Acme Global Operations Account', 'CUST-CORP-001', 'LIABILITY', 'USD', 'ACTIVE', 250000.0000, 250000.0000, 0),
('a0000000-0000-0000-0000-000000000003', 'AC-CUST-CORP-B02', 'Starlight Holdings Operating Account', 'CUST-CORP-002', 'LIABILITY', 'USD', 'ACTIVE', 180000.0000, 180000.0000, 0),
('a0000000-0000-0000-0000-000000000004', 'AC-CUST-RETL-C03', 'Apex Trading Reserve Account', 'CUST-RETL-003', 'LIABILITY', 'USD', 'ACTIVE', 50000.0000, 50000.0000, 0),
('a0000000-0000-0000-0000-000000000005', 'AC-FEE-REVENUE-01', 'Transaction Processing Fee Income', 'SYS-REV-01', 'REVENUE', 'USD', 'ACTIVE', 0.0000, 0.0000, 0),
('a0000000-0000-0000-0000-000000000006', 'AC-OPER-EXPENSE-01', 'Operational Processing Expense', 'SYS-EXP-01', 'EXPENSE', 'USD', 'ACTIVE', 0.0000, 0.0000, 0);

-- 5. System Configurations
INSERT INTO system_configurations (config_key, config_value, description) VALUES
('TRANSACTION_MAX_LIMIT', '1000000.00', 'Maximum single transaction value without dual authorization'),
('IDEMPOTENCY_TTL_HOURS', '24', 'Time to live for stored idempotency keys'),
('SYSTEM_CURRENCY_DEFAULT', 'USD', 'Base functional currency for ledger trial balance');
