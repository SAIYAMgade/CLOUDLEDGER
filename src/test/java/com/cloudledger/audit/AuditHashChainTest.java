package com.cloudledger.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditHashChainTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    @DisplayName("Should successfully verify valid cryptographic hash chain across audit records")
    void testValidHashChain() {
        String genesis = "0000000000000000000000000000000000000000000000000000000000000000";

        // Record 1
        String hash1 = AuditLogService.computeHash(genesis, "admin", "CREATE_ACCOUNT", "Account", "acc-1", "Created asset account");
        AuditLog log1 = new AuditLog(genesis, hash1, "admin", "CREATE_ACCOUNT", "Account", "acc-1", "corr-1", "127.0.0.1", "Created asset account");
        log1.setAuditId(1L);

        // Record 2
        String hash2 = AuditLogService.computeHash(hash1, "operator", "POST_TRANSFER", "Transaction", "tx-1", "Transfer USD 500");
        AuditLog log2 = new AuditLog(hash1, hash2, "operator", "POST_TRANSFER", "Transaction", "tx-1", "corr-2", "127.0.0.1", "Transfer USD 500");
        log2.setAuditId(2L);

        when(auditLogRepository.findAll()).thenReturn(List.of(log1, log2));

        Map<String, Object> verification = auditLogService.verifyChainIntegrity();

        assertTrue((Boolean) verification.get("verified"));
        assertEquals(2, verification.get("recordCount"));
    }

    @Test
    @DisplayName("Should detect tamper if any audit log payload is modified")
    void testTamperedHashChainDetected() {
        String genesis = "0000000000000000000000000000000000000000000000000000000000000000";

        String hash1 = AuditLogService.computeHash(genesis, "admin", "CREATE_ACCOUNT", "Account", "acc-1", "Original details");
        AuditLog log1 = new AuditLog(genesis, hash1, "admin", "CREATE_ACCOUNT", "Account", "acc-1", "corr-1", "127.0.0.1", "TAMPERED DETAILS!"); // Corrupted!
        log1.setAuditId(1L);

        when(auditLogRepository.findAll()).thenReturn(List.of(log1));

        Map<String, Object> verification = auditLogService.verifyChainIntegrity();

        assertFalse((Boolean) verification.get("verified"));
        assertEquals(1L, verification.get("tamperedRecordId"));
    }
}
