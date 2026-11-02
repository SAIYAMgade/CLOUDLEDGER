package com.cloudledger.reporting;

import com.cloudledger.common.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/daily-transactions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDailySummary() {
        Map<String, Object> summary = reportingService.getDailyTransactionSummary();
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/transaction-volume")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getVolumeByType() {
        Map<String, Long> volume = reportingService.getTransactionVolumeByType();
        return ResponseEntity.ok(ApiResponse.ok(volume));
    }

    @GetMapping("/failed-transactions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFailedTransactions() {
        List<Map<String, Object>> failed = reportingService.getFailedTransactions();
        return ResponseEntity.ok(ApiResponse.ok(failed));
    }

    @GetMapping(value = "/exports", produces = "text/csv")
    public ResponseEntity<String> exportCsv() {
        String csvData = reportingService.exportTransactionsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cloudledger_transactions.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
