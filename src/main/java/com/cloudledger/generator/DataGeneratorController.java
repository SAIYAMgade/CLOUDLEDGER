package com.cloudledger.generator;

import com.cloudledger.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/operations/synthetic-data")
public class DataGeneratorController {

    private final SyntheticDataGeneratorService dataGeneratorService;

    public DataGeneratorController(SyntheticDataGeneratorService dataGeneratorService) {
        this.dataGeneratorService = dataGeneratorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerGeneration(
            @RequestParam(defaultValue = "100") int accounts,
            @RequestParam(defaultValue = "500") int transactions,
            @RequestParam(defaultValue = "42") long seed) {

        Map<String, Object> result = dataGeneratorService.generateData(accounts, transactions, seed);
        return ResponseEntity.ok(ApiResponse.ok("Synthetic dataset generated", result));
    }
}
