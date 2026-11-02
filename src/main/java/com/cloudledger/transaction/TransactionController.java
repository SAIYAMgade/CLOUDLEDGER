package com.cloudledger.transaction;

import com.cloudledger.common.ApiResponse;
import com.cloudledger.transaction.dto.TransactionRequest;
import com.cloudledger.transaction.dto.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'FINANCE_OPERATOR')")
    public ResponseEntity<ApiResponse<TransactionResponse>> submitTransaction(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransactionRequest request) {

        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            idempotencyKey = UUID.randomUUID().toString();
        }

        TransactionResponse response = transactionService.processTransaction(request, idempotencyKey);

        HttpHeaders headers = new HttpHeaders();
        if (response.isIdempotentReplay()) {
            headers.add("Idempotent-Replay", "true");
        }

        HttpStatus status = response.getStatus() == TransactionStatus.POSTED ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).headers(headers).body(ApiResponse.ok("Transaction processed", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> listTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Page<TransactionResponse> result = transactionService.listTransactions(status, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(@PathVariable("id") UUID id) {
        TransactionResponse response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<TransactionEvent>>> getTransactionHistory(@PathVariable("id") UUID id) {
        List<TransactionEvent> events = transactionService.getTransactionEvents(id);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<ApiResponse<List<TransactionEvent>>> getEvents(@PathVariable("id") UUID id) {
        return getTransactionHistory(id);
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'FINANCE_APPROVER')")
    public ResponseEntity<ApiResponse<TransactionResponse>> reverseTransaction(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Manual administrative reversal";
        String reversedBy = (userDetails != null) ? userDetails.getUsername() : "SYSTEM";
        TransactionResponse response = transactionService.reverseTransaction(id, reason, reversedBy);
        return ResponseEntity.ok(ApiResponse.ok("Transaction reversed", response));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'FINANCE_OPERATOR')")
    public ResponseEntity<ApiResponse<TransactionResponse>> retryTransaction(@PathVariable("id") UUID id) {
        TransactionResponse response = transactionService.retryTransaction(id);
        return ResponseEntity.ok(ApiResponse.ok("Transaction retry triggered", response));
    }
}
