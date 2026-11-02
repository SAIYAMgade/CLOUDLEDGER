export type AccountType = 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE';
export type AccountStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED' | 'PENDING_VERIFICATION';

export interface Account {
  accountId: string;
  accountNumber: string;
  accountName: string;
  customerReference: string;
  accountType: AccountType;
  currency: string;
  status: AccountStatus;
  availableBalance: number;
  ledgerBalance: number;
  createdAt: string;
  updatedAt: string;
}

export type TransactionType = 'TRANSFER' | 'DEPOSIT' | 'WITHDRAWAL' | 'PAYMENT' | 'REVERSAL';
export type TransactionStatus =
  | 'RECEIVED'
  | 'VALIDATING'
  | 'PENDING'
  | 'PROCESSING'
  | 'POSTED'
  | 'FAILED'
  | 'REQUIRES_REVIEW'
  | 'COMPENSATING'
  | 'REVERSED';

export interface Transaction {
  transactionId: string;
  externalReference: string;
  sourceAccountId: string | null;
  destinationAccountId: string | null;
  amount: number;
  currency: string;
  transactionType: TransactionType;
  status: TransactionStatus;
  idempotencyKey: string;
  description: string | null;
  errorCode: string | null;
  errorMessage: string | null;
  createdAt: string;
  completedAt: string | null;
  idempotentReplay?: boolean;
}

export interface TransactionEvent {
  eventId: string;
  transactionId: string;
  eventType: string;
  payload: string;
  createdAt: string;
}

export interface JournalLine {
  accountId: string;
  direction: 'DEBIT' | 'CREDIT';
  amount: number;
  currency: string;
}

export interface JournalEntry {
  journalId: string;
  transactionId: string | null;
  journalNumber: string;
  description: string;
  status: 'DRAFT' | 'POSTED' | 'REVERSED';
  postedAt: string | null;
  createdAt: string;
  createdBy: string;
  lines: JournalLine[];
}

export interface TrialBalanceItem {
  accountId: string;
  accountNumber: string;
  accountName: string;
  accountType: string;
  debitAmount: number;
  creditAmount: number;
}

export interface TrialBalance {
  asOfDate: string;
  currency: string;
  totalDebits: number;
  totalCredits: number;
  balanced: boolean;
  items: TrialBalanceItem[];
}

export interface SagaStep {
  stepId: string;
  stepName: string;
  status: string;
  stepOrder: number;
  startedAt: string;
  completedAt: string | null;
  errorMessage: string | null;
}

export interface SagaInstance {
  sagaId: string;
  correlationId: string;
  sagaType: string;
  currentState: string;
  status: string;
  retryCount: number;
  steps: SagaStep[];
  createdAt: string;
  updatedAt: string;
}

export interface AuditLog {
  auditId: number;
  prevHash: string;
  currHash: string;
  userId: string;
  action: string;
  entityType: string;
  entityId: string;
  correlationId: string;
  ipAddress: string;
  details: string;
  createdAt: string;
}

export interface OperationsSummary {
  totalAccounts: number;
  totalTransactions: number;
  postedTransactions: number;
  failedTransactions: number;
  pendingTransactions: number;
  pendingOutboxEvents: number;
  totalSagaInstances: number;
  auditChainVerification: {
    verified: boolean;
    recordCount: number;
    latestHash?: string;
    message?: string;
    tamperedRecordId?: number;
  };
}
