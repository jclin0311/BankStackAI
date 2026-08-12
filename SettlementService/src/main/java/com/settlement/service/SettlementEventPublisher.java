package com.settlement.service;

import com.events.billpay.BillBatchSubmittedEvent;
import com.events.billpay.BillBatchRetryEvent;
import com.events.billpay.BillpayStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;   // Jackson from Spring Boot

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Failed to serialize {} event to JSON", event.getClass().getSimpleName(), e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    public void publishBatchSubmitted(BillBatchSubmittedEvent event) {
        log.info("Emitting bill.batch.submitted for batchId={}", event.batchId());
        kafkaTemplate.send(
                "bill.batch.submitted",
                event.batchId().toString(),      // key
                toJson(event)                    // value as String
        );
    }

    public void publishDlq(UUID batchId, String error) {
        log.info("Emitting bill.batch.dlq for batchId={} error={}", batchId, error);

        BillBatchRetryEvent event = new BillBatchRetryEvent(
                batchId,
                4,
                error,
                OffsetDateTime.now()
        );

        kafkaTemplate.send(
                "bill.batch.dlq",
                batchId.toString(),              // key
                toJson(event)                    // value as String
        );
    }

    public void publishBillpayStatus(BillpayStatusEvent event) {
        log.info("Emitting billpay.status for paymentId={} batchId={} status={}",
                event.paymentId(), event.batchId(), event.status());

        kafkaTemplate.send(
                "billpay.status",
                event.paymentId().toString(),    // key
                toJson(event)                    // value as String
        );
    }

    public void publishBatchRetry(BillBatchRetryEvent event) {
        log.info("Emitting bill.batch.retry for batchId={} attempt={}",
                event.batchId(), event.attemptNumber());

        kafkaTemplate.send(
                "bill.batch.retry",
                event.batchId().toString(),      // key
                toJson(event)                    // value as String
        );
    }
}
