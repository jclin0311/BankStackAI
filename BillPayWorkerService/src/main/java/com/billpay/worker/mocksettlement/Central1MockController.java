package com.billpay.worker.mocksettlement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/mock/central1")
@RequiredArgsConstructor
@Slf4j
public class Central1MockController {

    private final Central1Simulator central1Simulator;

    /**
     * Trigger Central 1 pain.002 simulation for a given batch.
     *
     * Example:
     * POST /api/mock/central1/pain002/3fa85f64-5717-4562-b3fc-2c963f66afa6
     */
    @PostMapping("/pain002/{batchId}")
    public ResponseEntity<String> simulatePain002(@PathVariable UUID batchId) {
        log.info("Received request to simulate pain.002 for batchId={}", batchId);

        central1Simulator.simulatePain002ForBatch(batchId);

        return ResponseEntity.accepted()
                .body("Pain.002 simulation triggered for batchId=" + batchId);
    }
}
