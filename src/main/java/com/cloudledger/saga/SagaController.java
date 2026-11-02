package com.cloudledger.saga;

import com.cloudledger.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/operations/sagas")
public class SagaController {

    private final SagaInstanceRepository sagaInstanceRepository;

    public SagaController(SagaInstanceRepository sagaInstanceRepository) {
        this.sagaInstanceRepository = sagaInstanceRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SagaInstance>>> listSagas() {
        List<SagaInstance> sagas = sagaInstanceRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok(sagas));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SagaInstance>> getSaga(@PathVariable("id") UUID id) {
        return sagaInstanceRepository.findById(id)
                .map(s -> ResponseEntity.ok(ApiResponse.ok(s)))
                .orElse(ResponseEntity.notFound().build());
    }
}
