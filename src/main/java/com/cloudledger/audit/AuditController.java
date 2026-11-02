package com.cloudledger.audit;

import com.cloudledger.common.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditController {

    private final AuditLogService auditLogService;

    public AuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AUDITOR')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        Page<AuditLog> result = auditLogService.listAuditLogs(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/verify")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AUDITOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyChain() {
        Map<String, Object> result = auditLogService.verifyChainIntegrity();
        return ResponseEntity.ok(ApiResponse.ok("Chain integrity check evaluated", result));
    }
}
