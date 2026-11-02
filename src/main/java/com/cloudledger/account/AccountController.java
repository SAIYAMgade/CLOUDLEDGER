package com.cloudledger.account;

import com.cloudledger.account.dto.AccountResponse;
import com.cloudledger.account.dto.CreateAccountRequest;
import com.cloudledger.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'FINANCE_OPERATOR')")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@Valid @RequestBody CreateAccountRequest req) {
        AccountResponse response = accountService.createAccount(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Account created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> listAccounts() {
        List<AccountResponse> accounts = accountService.listAccounts();
        return ResponseEntity.ok(ApiResponse.ok(accounts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable("id") UUID id) {
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccountBalance(@PathVariable("id") UUID id) {
        AccountResponse response = accountService.getAccountById(id);
        Map<String, Object> balanceInfo = Map.of(
            "accountId", response.getAccountId(),
            "accountNumber", response.getAccountNumber(),
            "currency", response.getCurrency(),
            "availableBalance", response.getAvailableBalance(),
            "ledgerBalance", response.getLedgerBalance()
        );
        return ResponseEntity.ok(ApiResponse.ok(balanceInfo));
    }

    @PostMapping("/{id}/freeze")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'FINANCE_APPROVER')")
    public ResponseEntity<ApiResponse<AccountResponse>> freezeAccount(@PathVariable("id") UUID id,
                                                                      @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Administrative freeze";
        AccountResponse response = accountService.freezeAccount(id, reason);
        return ResponseEntity.ok(ApiResponse.ok("Account frozen successfully", response));
    }

    @PostMapping("/{id}/unfreeze")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'FINANCE_APPROVER')")
    public ResponseEntity<ApiResponse<AccountResponse>> unfreezeAccount(@PathVariable("id") UUID id) {
        AccountResponse response = accountService.unfreezeAccount(id);
        return ResponseEntity.ok(ApiResponse.ok("Account unfrozen successfully", response));
    }
}
