package com.cloudledger.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog recordAudit(String userId, String action, String entityType, String entityId,
                                String correlationId, String ipAddress, String details) {
        String prevHash = auditLogRepository.findTopByOrderByAuditIdDesc()
                .map(AuditLog::getCurrHash)
                .orElse(GENESIS_HASH);

        String currHash = computeHash(prevHash, userId, action, entityType, entityId, details);

        AuditLog logEntry = new AuditLog(prevHash, currHash, userId, action, entityType, entityId, correlationId, ipAddress, details);
        AuditLog saved = auditLogRepository.save(logEntry);
        log.debug("Recorded tamper-evident audit record id={}, hash={}", saved.getAuditId(), saved.getCurrHash());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByAuditIdDesc(pageable);
    }

    /**
     * Verifies the cryptographic integrity of the entire audit log chain.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> verifyChainIntegrity() {
        List<AuditLog> allLogs = auditLogRepository.findAll();
        if (allLogs.isEmpty()) {
            return Map.of("verified", true, "recordCount", 0, "message", "Audit log is empty");
        }

        String expectedPrevHash = GENESIS_HASH;
        for (int i = 0; i < allLogs.size(); i++) {
            AuditLog entry = allLogs.get(i);

            // 1. Check prev_hash matches previous record's curr_hash
            if (!entry.getPrevHash().equalsIgnoreCase(expectedPrevHash)) {
                return Map.of(
                    "verified", false,
                    "tamperedRecordId", entry.getAuditId(),
                    "reason", "PrevHash mismatch at record " + entry.getAuditId()
                );
            }

            // 2. Recompute curr_hash and verify
            String recomputedHash = computeHash(entry.getPrevHash(), entry.getUserId(), entry.getAction(),
                    entry.getEntityType(), entry.getEntityId(), entry.getDetails());

            if (!entry.getCurrHash().equalsIgnoreCase(recomputedHash)) {
                return Map.of(
                    "verified", false,
                    "tamperedRecordId", entry.getAuditId(),
                    "reason", "Content hash corrupted at record " + entry.getAuditId()
                );
            }

            expectedPrevHash = entry.getCurrHash();
        }

        return Map.of(
            "verified", true,
            "recordCount", allLogs.size(),
            "latestHash", expectedPrevHash,
            "message", "Cryptographic chain integrity verified successfully"
        );
    }

    public static String computeHash(String prevHash, String userId, String action, String entityType, String entityId, String details) {
        try {
            String raw = prevHash + ":" + userId + ":" + action + ":" + entityType + ":" + entityId + ":" + (details != null ? details : "");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }
}
