package com.settlement.kafka;

import com.billpay.dto.Pain002FileMessage;
import com.events.billpay.BillBatchReadyEvent;
import com.events.billpay.BillBatchRetryEvent;
import com.events.billpay.Pain002Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.settlement.service.SettlementProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementKafkaListeners {

    private final SettlementProcessor settlementProcessor;
    private final ObjectMapper objectMapper; 

    @KafkaListener(topics = "bill.batch.ready", groupId = "settlement-service")
    public void onBatchReady(String payload) {
        try {
            BillBatchReadyEvent event =
                    objectMapper.readValue(payload, BillBatchReadyEvent.class);

            log.info("Received bill.batch.ready for batchId={}", event.batchId());
            settlementProcessor.processNewBatch(event);
        } catch (Exception e) {
            log.error("Failed to parse BillBatchReadyEvent from payload: {}", payload, e);
            throw new RuntimeException("Failed to handle bill.batch.ready", e);
        }
    }

    @KafkaListener(topics = "bill.batch.retry", groupId = "settlement-service")
    public void onBatchRetry(String payload) {
        try {
            BillBatchRetryEvent event =
                    objectMapper.readValue(payload, BillBatchRetryEvent.class);

            log.info("Received bill.batch.retry for batchId={}", event.batchId());
            settlementProcessor.retryBatch(event);
        } catch (Exception e) {
            log.error("Failed to parse BillBatchRetryEvent from payload: {}", payload, e);
            throw new RuntimeException("Failed to handle bill.batch.retry", e);
        }
    }

    @KafkaListener(topics = "central1.pain002", groupId = "settlement-service")
    public void onPain002(String payload) {
        try {
            Pain002FileMessage file =
                    objectMapper.readValue(payload, Pain002FileMessage.class);

            log.info("Received pain.002 file for batchId={} with {} items",
                    file.batchId(), file.items().size());

            for (Pain002Message msg : file.items()) {
                settlementProcessor.handlePain002(msg);
            }
        } catch (Exception e) {
            log.error("Failed to parse Pain002FileMessage from payload: {}", payload, e);
            throw new RuntimeException("Failed to handle central1.pain002", e);
        }
    }
}
