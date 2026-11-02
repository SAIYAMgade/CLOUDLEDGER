package com.cloudledger.ledger;

import com.cloudledger.common.ApiResponse;
import com.cloudledger.ledger.dto.GeneralLedgerDTO;
import com.cloudledger.ledger.dto.JournalEntryDTO;
import com.cloudledger.ledger.dto.TrialBalanceDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/journals")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'FINANCE_OPERATOR')")
    public ResponseEntity<ApiResponse<JournalEntryDTO>> createJournal(@Valid @RequestBody JournalEntryDTO req,
                                                                      @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            req.setCreatedBy(userDetails.getUsername());
        }
        JournalEntry created = ledgerService.createJournalEntry(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Journal entry created in DRAFT status", JournalEntryDTO.from(created)));
    }

    @PostMapping("/journals/{id}/post")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'FINANCE_OPERATOR', 'FINANCE_APPROVER')")
    public ResponseEntity<ApiResponse<JournalEntryDTO>> postJournal(@PathVariable("id") UUID id) {
        JournalEntry posted = ledgerService.postJournal(id);
        return ResponseEntity.ok(ApiResponse.ok("Journal entry posted to authoritative ledger", JournalEntryDTO.from(posted)));
    }

    @PostMapping("/journals/{id}/reverse")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'FINANCE_APPROVER')")
    public ResponseEntity<ApiResponse<JournalEntryDTO>> reverseJournal(@PathVariable("id") UUID id,
                                                                       @RequestBody(required = false) Map<String, String> body,
                                                                       @AuthenticationPrincipal UserDetails userDetails) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Manual administrative reversal";
        String reversedBy = (userDetails != null) ? userDetails.getUsername() : "SYSTEM";
        JournalEntry reversed = ledgerService.reverseJournal(id, reason, reversedBy);
        return ResponseEntity.ok(ApiResponse.ok("Journal entry reversed successfully", JournalEntryDTO.from(reversed)));
    }

    @GetMapping("/trial-balance")
    public ResponseEntity<ApiResponse<TrialBalanceDTO>> getTrialBalance() {
        TrialBalanceDTO tb = ledgerService.generateTrialBalance();
        return ResponseEntity.ok(ApiResponse.ok(tb));
    }

    @GetMapping("/general-ledger/{accountId}")
    public ResponseEntity<ApiResponse<GeneralLedgerDTO>> getGeneralLedger(@PathVariable("accountId") UUID accountId) {
        GeneralLedgerDTO gl = ledgerService.getGeneralLedger(accountId);
        return ResponseEntity.ok(ApiResponse.ok(gl));
    }
}
