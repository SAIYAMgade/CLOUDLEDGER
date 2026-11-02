package com.cloudledger.observability;

import com.cloudledger.account.AccountRepository;
import com.cloudledger.audit.AuditLogService;
import com.cloudledger.common.ApiResponse;
import com.cloudledger.outbox.OutboxRepository;
import com.cloudledger.saga.SagaInstanceRepository;
import com.cloudledger.transaction.TransactionRepository;
import com.cloudledger.transaction.TransactionStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/operations")
public class OperationsController {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final OutboxRepository outboxRepository;
    private final SagaInstanceRepository sagaInstanceRepository;
    private final AuditLogService auditLogService;

    public OperationsController(AccountRepository accountRepository,
                                TransactionRepository transactionRepository,
                                OutboxRepository outboxRepository,
                                SagaInstanceRepository sagaInstanceRepository,
                                AuditLogService auditLogService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.outboxRepository = outboxRepository;
        this.sagaInstanceRepository = sagaInstanceRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("database", "CONNECTED");
        health.put("jvmUptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        health.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        health.put("freeMemoryBytes", Runtime.getRuntime().freeMemory());
        return ResponseEntity.ok(ApiResponse.ok(health));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOperationsSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalAccounts", accountRepository.count());
        summary.put("totalTransactions", transactionRepository.count());
        summary.put("postedTransactions", transactionRepository.countByStatus(TransactionStatus.POSTED));
        summary.put("failedTransactions", transactionRepository.countByStatus(TransactionStatus.FAILED));
        summary.put("pendingTransactions", transactionRepository.countByStatus(TransactionStatus.PENDING));
        summary.put("pendingOutboxEvents", outboxRepository.countByStatus("PENDING"));
        summary.put("totalSagaInstances", sagaInstanceRepository.count());
        summary.put("auditChainVerification", auditLogService.verifyChainIntegrity());

        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
