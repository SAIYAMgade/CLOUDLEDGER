package com.cloudledger.transaction;

import com.cloudledger.common.exceptions.IdempotencyConflictException;
import com.cloudledger.transaction.dto.TransactionRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRepository idempotencyRepository, ObjectMapper objectMapper) {
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }

    public String computePayloadHash(TransactionRequest request) {
        try {
            // Canonical serialization: sorted/deterministic
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute SHA-256 idempotency hash", e);
        }
    }

    @Transactional
    public Optional<TransactionIdempotency> checkIdempotency(String idempotencyKey, String payloadHash) {
        Optional<TransactionIdempotency> existing = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            TransactionIdempotency record = existing.get();
            if (!record.getPayloadHash().equalsIgnoreCase(payloadHash)) {
                throw new IdempotencyConflictException(
                    "Idempotency key '" + idempotencyKey + "' was previously used with a different request payload"
                );
            }
            return Optional.of(record);
        }
        return Optional.empty();
    }

    @Transactional
    public TransactionIdempotency registerIdempotency(String idempotencyKey, String payloadHash, UUID transactionId) {
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        TransactionIdempotency record = new TransactionIdempotency(
            idempotencyKey, payloadHash, transactionId, "PROCESSING", expiresAt
        );
        return idempotencyRepository.save(record);
    }

    @Transactional
    public void markCompleted(String idempotencyKey, String responsePayload) {
        idempotencyRepository.findByIdempotencyKey(idempotencyKey).ifPresent(record -> {
            record.setStatus("COMPLETED");
            record.setResponsePayload(responsePayload);
            idempotencyRepository.save(record);
        });
    }
}
