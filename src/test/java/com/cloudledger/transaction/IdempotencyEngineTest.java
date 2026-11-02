package com.cloudledger.transaction;

import com.cloudledger.common.exceptions.IdempotencyConflictException;
import com.cloudledger.transaction.dto.TransactionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyEngineTest {

    @Mock
    private IdempotencyRepository idempotencyRepository;

    private ObjectMapper objectMapper;
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        idempotencyService = new IdempotencyService(idempotencyRepository, objectMapper);
    }

    @Test
    @DisplayName("Should detect identical idempotency request and allow safe replay")
    void testSameKeySamePayload() {
        String key = "KEY-TEST-123";
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("100.0000"));
        req.setCurrency("USD");
        req.setTransactionType(TransactionType.TRANSFER);

        String hash = idempotencyService.computePayloadHash(req);

        TransactionIdempotency record = new TransactionIdempotency(key, hash, UUID.randomUUID(), "COMPLETED", Instant.now().plusSeconds(3600));
        when(idempotencyRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(record));

        Optional<TransactionIdempotency> result = idempotencyService.checkIdempotency(key, hash);

        assertTrue(result.isPresent());
        assertEquals(key, result.get().getIdempotencyKey());
    }

    @Test
    @DisplayName("Should throw IdempotencyConflictException when same key is used with differing payload")
    void testSameKeyDifferentPayloadConflict() {
        String key = "KEY-TEST-123";

        TransactionRequest req1 = new TransactionRequest();
        req1.setAmount(new BigDecimal("100.0000"));
        req1.setCurrency("USD");
        req1.setTransactionType(TransactionType.TRANSFER);

        TransactionRequest req2 = new TransactionRequest();
        req2.setAmount(new BigDecimal("200.0000")); // Different amount!
        req2.setCurrency("USD");
        req2.setTransactionType(TransactionType.TRANSFER);

        String hash1 = idempotencyService.computePayloadHash(req1);
        String hash2 = idempotencyService.computePayloadHash(req2);

        TransactionIdempotency record1 = new TransactionIdempotency(key, hash1, UUID.randomUUID(), "COMPLETED", Instant.now().plusSeconds(3600));
        when(idempotencyRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(record1));

        IdempotencyConflictException ex = assertThrows(IdempotencyConflictException.class, () ->
            idempotencyService.checkIdempotency(key, hash2)
        );

        assertTrue(ex.getMessage().contains("was previously used with a different request payload"));
    }
}
