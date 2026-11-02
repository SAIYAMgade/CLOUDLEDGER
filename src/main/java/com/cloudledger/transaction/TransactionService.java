package com.cloudledger.transaction;

import com.cloudledger.common.CorrelationFilter;
import com.cloudledger.common.MoneyUtils;
import com.cloudledger.common.exceptions.BusinessException;
import com.cloudledger.common.exceptions.ResourceNotFoundException;
import com.cloudledger.ledger.JournalEntry;
import com.cloudledger.ledger.JournalEntryRepository;
import com.cloudledger.ledger.LedgerService;
import com.cloudledger.outbox.OutboxService;
import com.cloudledger.saga.TransferSagaCoordinator;
import com.cloudledger.transaction.dto.TransactionRequest;
import com.cloudledger.transaction.dto.TransactionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository transactionEventRepository;
    private final IdempotencyService idempotencyService;
    private final TransferSagaCoordinator sagaCoordinator;
    private final LedgerService ledgerService;
    private final JournalEntryRepository journalEntryRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public TransactionService(TransactionRepository transactionRepository,
                              TransactionEventRepository transactionEventRepository,
                              IdempotencyService idempotencyService,
                              TransferSagaCoordinator sagaCoordinator,
                              LedgerService ledgerService,
                              JournalEntryRepository journalEntryRepository,
                              OutboxService outboxService,
                              ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.transactionEventRepository = transactionEventRepository;
        this.idempotencyService = idempotencyService;
        this.sagaCoordinator = sagaCoordinator;
        this.ledgerService = ledgerService;
        this.journalEntryRepository = journalEntryRepository;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new BusinessException("MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is mandatory for transaction processing");
        }

        String payloadHash = idempotencyService.computePayloadHash(request);

        // Check if an existing idempotency record exists
        Optional<TransactionIdempotency> existing = idempotencyService.checkIdempotency(idempotencyKey, payloadHash);
        if (existing.isPresent()) {
            TransactionIdempotency record = existing.get();
            Transaction existingTx = transactionRepository.findById(record.getTransactionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for existing key: " + idempotencyKey));
            
            log.info("Idempotent replay detected for key {}: returning existing transaction {}", idempotencyKey, existingTx.getTransactionId());
            TransactionResponse resp = TransactionResponse.from(existingTx);
            resp.setIdempotentReplay(true);
            return resp;
        }

        // Validate basic request fields
        if (request.getAmount() == null || request.getAmount().compareTo(MoneyUtils.ZERO) <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Transaction amount must be strictly greater than zero");
        }

        // Create transaction in RECEIVED state
        Transaction tx = new Transaction();
        tx.setExternalReference(request.getExternalReference() != null ? request.getExternalReference() : "EXT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setSourceAccountId(request.getSourceAccountId());
        tx.setDestinationAccountId(request.getDestinationAccountId());
        tx.setAmount(request.getAmount());
        tx.setCurrency(request.getCurrency().toUpperCase());
        tx.setTransactionType(request.getTransactionType());
        tx.setStatus(TransactionStatus.RECEIVED);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setDescription(request.getDescription());
        tx.setRequestReference(CorrelationFilter.getCurrentCorrelationId());

        Transaction savedTx = transactionRepository.save(tx);

        // Register idempotency key
        idempotencyService.registerIdempotency(idempotencyKey, payloadHash, savedTx.getTransactionId());

        // Emit creation event
        transactionEventRepository.save(new TransactionEvent(
            savedTx.getTransactionId(), "TRANSACTION_RECEIVED", "Transaction received with amount " + savedTx.getAmount()
        ));

        // Atomically record outbox event
        outboxService.recordEvent(
            "Transaction",
            savedTx.getTransactionId().toString(),
            "transaction.created",
            CorrelationFilter.getCurrentCorrelationId(),
            Map.of("transactionId", savedTx.getTransactionId(), "amount", savedTx.getAmount(), "currency", savedTx.getCurrency())
        );

        // Execute Saga Orchestration for transfer execution
        sagaCoordinator.executeTransferSaga(savedTx);

        // Mark idempotency completed
        try {
            idempotencyService.markCompleted(idempotencyKey, objectMapper.writeValueAsString(TransactionResponse.from(savedTx)));
        } catch (JsonProcessingException ignored) {}

        return TransactionResponse.from(savedTx);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));
        return TransactionResponse.from(tx);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listTransactions(TransactionStatus status, Pageable pageable) {
        Page<Transaction> page = (status != null) ?
                transactionRepository.findByStatus(status, pageable) :
                transactionRepository.findAll(pageable);
        return page.map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<TransactionEvent> getTransactionEvents(UUID transactionId) {
        return transactionEventRepository.findByTransactionIdOrderByCreatedAtAsc(transactionId);
    }

    @Transactional
    public TransactionResponse reverseTransaction(UUID transactionId, String reason, String reversedBy) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));

        if (tx.getStatus() != TransactionStatus.POSTED) {
            throw new BusinessException("INVALID_STATE_FOR_REVERSAL", "Only POSTED transactions can be reversed");
        }

        // Find associated journal entry
        JournalEntry journal = journalEntryRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new BusinessException("JOURNAL_NOT_FOUND", "No journal entry linked to transaction"));

        // Reverse the journal entry
        ledgerService.reverseJournal(journal.getJournalId(), reason, reversedBy);

        tx.setStatus(TransactionStatus.REVERSED);
        tx.setUpdatedAt(Instant.now());
        Transaction saved = transactionRepository.save(tx);

        transactionEventRepository.save(new TransactionEvent(
            transactionId, "TRANSACTION_REVERSED", "Reversed transaction: " + reason
        ));

        outboxService.recordEvent(
            "Transaction",
            tx.getTransactionId().toString(),
            "transaction.reversed",
            CorrelationFilter.getCurrentCorrelationId(),
            Map.of("transactionId", tx.getTransactionId(), "reason", reason)
        );

        return TransactionResponse.from(saved);
    }

    @Transactional
    public TransactionResponse retryTransaction(UUID transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));

        if (tx.getStatus() != TransactionStatus.FAILED) {
            throw new BusinessException("CANNOT_RETRY", "Only FAILED transactions can be retried");
        }

        tx.setStatus(TransactionStatus.RECEIVED);
        tx.setErrorCode(null);
        tx.setErrorMessage(null);
        tx.setUpdatedAt(Instant.now());
        transactionRepository.save(tx);

        sagaCoordinator.executeTransferSaga(tx);
        return TransactionResponse.from(tx);
    }
}
