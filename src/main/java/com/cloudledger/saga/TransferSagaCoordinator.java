package com.cloudledger.saga;

import com.cloudledger.account.Account;
import com.cloudledger.account.AccountRepository;
import com.cloudledger.account.AccountService;
import com.cloudledger.account.AccountStatus;
import com.cloudledger.common.exceptions.AccountFrozenException;
import com.cloudledger.common.exceptions.BusinessException;
import com.cloudledger.common.exceptions.InsufficientBalanceException;
import com.cloudledger.ledger.EntryDirection;
import com.cloudledger.ledger.JournalEntry;
import com.cloudledger.ledger.LedgerService;
import com.cloudledger.ledger.dto.JournalEntryDTO;
import com.cloudledger.ledger.dto.JournalLineDTO;
import com.cloudledger.outbox.OutboxService;
import com.cloudledger.transaction.Transaction;
import com.cloudledger.transaction.TransactionRepository;
import com.cloudledger.transaction.TransactionStatus;
import com.cloudledger.transaction.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TransferSagaCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TransferSagaCoordinator.class);

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final LedgerService ledgerService;
    private final TransactionRepository transactionRepository;
    private final OutboxService outboxService;

    public TransferSagaCoordinator(SagaInstanceRepository sagaInstanceRepository,
                                   SagaStepRepository sagaStepRepository,
                                   AccountService accountService,
                                   AccountRepository accountRepository,
                                   LedgerService ledgerService,
                                   TransactionRepository transactionRepository,
                                   OutboxService outboxService) {
        this.sagaInstanceRepository = sagaInstanceRepository;
        this.sagaStepRepository = sagaStepRepository;
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.ledgerService = ledgerService;
        this.transactionRepository = transactionRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public void executeTransferSaga(Transaction tx) {
        String correlationId = tx.getTransactionId().toString();
        log.info("Starting Transfer Saga for Transaction id={}, ref={}", tx.getTransactionId(), tx.getExternalReference());

        SagaInstance saga = new SagaInstance();
        saga.setCorrelationId(correlationId);
        saga.setSagaType(tx.getTransactionType().name());
        saga.setCurrentState("INITIALIZED");
        saga.setPayload("{\"amount\":\"" + tx.getAmount() + "\",\"currency\":\"" + tx.getCurrency() + "\"}");
        saga.setStatus(SagaStatus.RUNNING);
        saga = sagaInstanceRepository.save(saga);

        boolean reservationCompleted = false;

        try {
            // STEP 1: Account & Currency Validation
            SagaStep step1 = new SagaStep("VALIDATE_ACCOUNTS", 1);
            saga.addStep(step1);
            validateAccountsForTransaction(tx);
            step1.setStatus("COMPLETED");
            step1.setCompletedAt(Instant.now());
            sagaStepRepository.save(step1);

            // STEP 2: Reserve Source Funds (Pessimistic Lock)
            if (tx.getSourceAccountId() != null) {
                SagaStep step2 = new SagaStep("RESERVE_FUNDS", 2);
                saga.addStep(step2);
                accountService.reserveFunds(tx.getSourceAccountId(), tx.getAmount());
                reservationCompleted = true;
                step2.setStatus("COMPLETED");
                step2.setCompletedAt(Instant.now());
                sagaStepRepository.save(step2);
            }

            // STEP 3: Post Double-Entry Journal to Ledger
            SagaStep step3 = new SagaStep("POST_JOURNAL_ENTRY", 3);
            saga.addStep(step3);
            JournalEntry journal = postDoubleEntryForTransaction(tx);
            step3.setStatus("COMPLETED");
            step3.setCompletedAt(Instant.now());
            sagaStepRepository.save(step3);

            // STEP 4: Commit Ledger Balances
            SagaStep step4 = new SagaStep("COMMIT_BALANCES", 4);
            saga.addStep(step4);
            if (tx.getSourceAccountId() != null) {
                accountService.commitDebit(tx.getSourceAccountId(), tx.getAmount());
            }
            if (tx.getDestinationAccountId() != null) {
                accountService.commitCredit(tx.getDestinationAccountId(), tx.getAmount());
            }
            step4.setStatus("COMPLETED");
            step4.setCompletedAt(Instant.now());
            sagaStepRepository.save(step4);

            // Finalize Transaction
            tx.setStatus(TransactionStatus.POSTED);
            tx.setCompletedAt(Instant.now());
            transactionRepository.save(tx);

            saga.setCurrentState("COMPLETED");
            saga.setStatus(SagaStatus.COMPLETED);
            saga.setUpdatedAt(Instant.now());
            sagaInstanceRepository.save(saga);

            // Outbox notification
            outboxService.recordEvent(
                "Transaction",
                tx.getTransactionId().toString(),
                "transaction.posted",
                correlationId,
                Map.of("transactionId", tx.getTransactionId(), "status", "POSTED", "amount", tx.getAmount())
            );

            log.info("Saga COMPLETED successfully for Transaction id={}", tx.getTransactionId());

        } catch (Exception ex) {
            log.error("Saga step failed for transaction {}: {}", tx.getTransactionId(), ex.getMessage());

            // Check if compensation is required
            if (reservationCompleted && tx.getSourceAccountId() != null) {
                executeCompensation(saga, tx);
            } else {
                saga.setCurrentState("FAILED");
                saga.setStatus(SagaStatus.FAILED);
                saga.setUpdatedAt(Instant.now());
                sagaInstanceRepository.save(saga);
            }

            tx.setStatus(TransactionStatus.FAILED);
            tx.setErrorCode(ex instanceof BusinessException ? ((BusinessException) ex).getErrorCode() : "SAGA_EXECUTION_FAILURE");
            tx.setErrorMessage(ex.getMessage());
            tx.setCompletedAt(Instant.now());
            transactionRepository.save(tx);

            outboxService.recordEvent(
                "Transaction",
                tx.getTransactionId().toString(),
                "transaction.failed",
                correlationId,
                Map.of("transactionId", tx.getTransactionId(), "status", "FAILED", "reason", ex.getMessage())
            );
        }
    }

    private void executeCompensation(SagaInstance saga, Transaction tx) {
        log.warn("Executing Saga Compensation for transaction {}", tx.getTransactionId());
        saga.setCurrentState("COMPENSATING");
        saga.setStatus(SagaStatus.COMPENSATING);
        sagaInstanceRepository.save(saga);

        SagaStep compStep = new SagaStep("COMPENSATE_RELEASE_RESERVED_FUNDS", 99);
        saga.addStep(compStep);

        try {
            accountService.releaseReservedFunds(tx.getSourceAccountId(), tx.getAmount());
            compStep.setStatus("COMPLETED");
            compStep.setCompletedAt(Instant.now());
            sagaStepRepository.save(compStep);

            saga.setCurrentState("COMPENSATED_FAILED");
            saga.setStatus(SagaStatus.FAILED);
            sagaInstanceRepository.save(saga);
            log.info("Saga compensation succeeded: reserved funds released for tx {}", tx.getTransactionId());
        } catch (Exception compEx) {
            log.error("Critical: Saga compensation failed for tx {}", tx.getTransactionId(), compEx);
            compStep.setStatus("FAILED");
            compStep.setErrorMessage(compEx.getMessage());
            sagaStepRepository.save(compStep);

            saga.setCurrentState("COMPENSATION_FAILED");
            saga.setStatus(SagaStatus.FAILED);
            sagaInstanceRepository.save(saga);
        }
    }

    private void validateAccountsForTransaction(Transaction tx) {
        if (tx.getTransactionType() == TransactionType.TRANSFER) {
            if (tx.getSourceAccountId() == null || tx.getDestinationAccountId() == null) {
                throw new BusinessException("INVALID_TRANSFER_PARTIES", "Source and destination accounts are required for transfers");
            }
            if (tx.getSourceAccountId().equals(tx.getDestinationAccountId())) {
                throw new BusinessException("IDENTICAL_ACCOUNTS", "Source and destination accounts must not be identical");
            }
        }

        if (tx.getSourceAccountId() != null) {
            Account src = accountRepository.findById(tx.getSourceAccountId())
                    .orElseThrow(() -> new BusinessException("SOURCE_ACCOUNT_NOT_FOUND", "Source account does not exist"));
            if (src.getStatus() != AccountStatus.ACTIVE) {
                throw new AccountFrozenException("Source account is not active: " + src.getStatus());
            }
            if (!src.getCurrency().equalsIgnoreCase(tx.getCurrency())) {
                throw new BusinessException("CURRENCY_MISMATCH", "Source account currency does not match transaction currency");
            }
        }

        if (tx.getDestinationAccountId() != null) {
            Account dest = accountRepository.findById(tx.getDestinationAccountId())
                    .orElseThrow(() -> new BusinessException("DEST_ACCOUNT_NOT_FOUND", "Destination account does not exist"));
            if (dest.getStatus() != AccountStatus.ACTIVE) {
                throw new AccountFrozenException("Destination account is not active: " + dest.getStatus());
            }
            if (!dest.getCurrency().equalsIgnoreCase(tx.getCurrency())) {
                throw new BusinessException("CURRENCY_MISMATCH", "Destination account currency does not match transaction currency");
            }
        }
    }

    private JournalEntry postDoubleEntryForTransaction(Transaction tx) {
        UUID srcAccId = tx.getSourceAccountId();
        UUID destAccId = tx.getDestinationAccountId();

        // Customer-to-customer transfer:
        // DEBIT source account liability (bank owes customer A less)
        // CREDIT destination account liability (bank owes customer B more)
        JournalLineDTO lineDebit = new JournalLineDTO(srcAccId, EntryDirection.DEBIT, tx.getAmount(), tx.getCurrency());
        JournalLineDTO lineCredit = new JournalLineDTO(destAccId, EntryDirection.CREDIT, tx.getAmount(), tx.getCurrency());

        JournalEntryDTO journalDTO = new JournalEntryDTO();
        journalDTO.setTransactionId(tx.getTransactionId());
        journalDTO.setDescription(tx.getDescription() != null ? tx.getDescription() : "Transfer Tx: " + tx.getTransactionId());
        journalDTO.setJournalNumber("JE-TX-" + tx.getTransactionId().toString().substring(0, 8).toUpperCase());
        journalDTO.setLines(List.of(lineDebit, lineCredit));
        journalDTO.setCreatedBy("TRANSFER_SAGA");

        JournalEntry created = ledgerService.createJournalEntry(journalDTO);
        return ledgerService.postJournal(created.getJournalId());
    }
}
