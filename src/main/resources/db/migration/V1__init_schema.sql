-- V1__init_schema.sql: Core Authoritative Schema for CloudLedger

-- 1. Identity & RBAC
CREATE TABLE roles (
    role_id VARCHAR(32) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role_id VARCHAR(32) NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

-- 2. Account Holders & Accounts
CREATE TABLE account_holders (
    holder_id UUID PRIMARY KEY,
    customer_reference VARCHAR(64) NOT NULL UNIQUE,
    full_name VARCHAR(128) NOT NULL,
    email VARCHAR(128) NOT NULL,
    kyc_status VARCHAR(32) NOT NULL DEFAULT 'VERIFIED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
    account_id UUID PRIMARY KEY,
    account_number VARCHAR(34) NOT NULL UNIQUE,
    account_name VARCHAR(128) NOT NULL,
    customer_reference VARCHAR(64) NOT NULL,
    account_type VARCHAR(32) NOT NULL, -- ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, FROZEN, CLOSED, PENDING_VERIFICATION
    available_balance NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    ledger_balance NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_positive_balances CHECK (
        (account_type IN ('ASSET', 'EXPENSE') AND available_balance >= 0) OR
        (account_type NOT IN ('ASSET', 'EXPENSE'))
    )
);

CREATE INDEX idx_accounts_customer_ref ON accounts(customer_reference);
CREATE INDEX idx_accounts_status ON accounts(status);

-- 3. Transactions & Idempotency
CREATE TABLE transactions (
    transaction_id UUID PRIMARY KEY,
    external_reference VARCHAR(64) UNIQUE,
    source_account_id UUID REFERENCES accounts(account_id),
    destination_account_id UUID REFERENCES accounts(account_id),
    amount NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL, -- TRANSFER, DEPOSIT, WITHDRAWAL, PAYMENT, REVERSAL
    status VARCHAR(32) NOT NULL DEFAULT 'RECEIVED', -- RECEIVED, VALIDATING, PENDING, PROCESSING, POSTED, FAILED, REQUIRES_REVIEW, COMPENSATING, REVERSED
    idempotency_key VARCHAR(128) NOT NULL,
    request_reference VARCHAR(128),
    description VARCHAR(255),
    error_code VARCHAR(64),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_source ON transactions(source_account_id);
CREATE INDEX idx_transactions_destination ON transactions(destination_account_id);

CREATE TABLE transaction_idempotency (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    payload_hash VARCHAR(64) NOT NULL,
    transaction_id UUID NOT NULL REFERENCES transactions(transaction_id),
    status VARCHAR(32) NOT NULL,
    response_payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE transaction_events (
    event_id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(transaction_id),
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Double-Entry Accounting Ledger
CREATE TABLE journal_entries (
    journal_id UUID PRIMARY KEY,
    transaction_id UUID UNIQUE REFERENCES transactions(transaction_id),
    journal_number VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT', -- DRAFT, POSTED, REVERSED
    posted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL
);

CREATE INDEX idx_journal_posted_at ON journal_entries(posted_at);

CREATE TABLE journal_entry_lines (
    line_id UUID PRIMARY KEY,
    journal_id UUID NOT NULL REFERENCES journal_entries(journal_id) ON DELETE RESTRICT,
    account_id UUID NOT NULL REFERENCES accounts(account_id) ON DELETE RESTRICT,
    direction VARCHAR(16) NOT NULL, -- DEBIT, CREDIT
    amount NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    line_order INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_journal_line_order UNIQUE (journal_id, line_order)
);

CREATE INDEX idx_journal_lines_account ON journal_entry_lines(account_id);

CREATE TABLE ledger_postings (
    posting_id UUID PRIMARY KEY,
    journal_id UUID NOT NULL REFERENCES journal_entries(journal_id),
    account_id UUID NOT NULL REFERENCES accounts(account_id),
    direction VARCHAR(16) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    running_balance NUMERIC(19,4) NOT NULL,
    posted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ledger_postings_account ON ledger_postings(account_id, posted_at);

CREATE TABLE reversal_entries (
    reversal_id UUID PRIMARY KEY,
    original_journal_id UUID NOT NULL UNIQUE REFERENCES journal_entries(journal_id),
    reversal_journal_id UUID NOT NULL UNIQUE REFERENCES journal_entries(journal_id),
    reason TEXT NOT NULL,
    reversed_by VARCHAR(64) NOT NULL,
    reversed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE account_balance_snapshots (
    snapshot_id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(account_id),
    snapshot_date DATE NOT NULL,
    opening_balance NUMERIC(19,4) NOT NULL,
    closing_balance NUMERIC(19,4) NOT NULL,
    total_debits NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_credits NUMERIC(19,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_account_snapshot_date UNIQUE (account_id, snapshot_date)
);

-- 5. Outbox, Inbox & Distributed Saga
CREATE TABLE outbox_events (
    outbox_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING', -- PENDING, PUBLISHED, FAILED
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dispatched_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);

CREATE TABLE consumer_inbox (
    message_id VARCHAR(128) NOT NULL,
    consumer_group VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id, consumer_group)
);

CREATE TABLE saga_instances (
    saga_id UUID PRIMARY KEY,
    correlation_id VARCHAR(64) NOT NULL UNIQUE,
    saga_type VARCHAR(64) NOT NULL,
    current_state VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'STARTED', -- STARTED, COMPLETED, COMPENSATING, FAILED
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE saga_steps (
    step_id UUID PRIMARY KEY,
    saga_id UUID NOT NULL REFERENCES saga_instances(saga_id) ON DELETE CASCADE,
    step_name VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    step_order INT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    error_message TEXT
);

-- 6. Tamper-Evident Audit Logs
CREATE TABLE audit_logs (
    audit_id BIGSERIAL PRIMARY KEY,
    prev_hash VARCHAR(64) NOT NULL,
    curr_hash VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(64),
    ip_address VARCHAR(45),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_created ON audit_logs(created_at);

-- 7. Notifications, Reports & Configurations
CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY,
    recipient VARCHAR(128) NOT NULL,
    channel VARCHAR(32) NOT NULL, -- EMAIL, SMS, WEBHOOK
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMPTZ
);

CREATE TABLE report_jobs (
    job_id UUID PRIMARY KEY,
    report_type VARCHAR(64) NOT NULL,
    parameters TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'QUEUED', -- QUEUED, GENERATING, COMPLETED, FAILED
    file_path VARCHAR(255),
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE TABLE system_configurations (
    config_key VARCHAR(64) PRIMARY KEY,
    config_value VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
