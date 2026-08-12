package com.payments.orch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.orch.domain.Payment;
import com.payments.orch.domain.ProcessedEvent;
import com.events.billpay.*;
import com.payments.orch.repo.PaymentRepo;
import com.payments.orch.repo.ProcessedEventRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmittedConsumer {
  private final PaymentRepo paymentRepo;
  private final ProcessedEventRepo processed;
  private final ObjectMapper om;

  @KafkaListener(topics="bill.batch.submitted", groupId="payment-api")
  @Transactional
  public void onMessage(String message) throws Exception {
    var evt = om.readValue(message, BillBatchSubmitted.class);

    String eventId = evt.getEventId();
    if (eventId == null || eventId.isBlank()) {
      // Option 1: batchId ko hi eventId treat karo (logical, kyunki ye batch-level event hai)
      eventId = evt.getBatchId();
      // ya Option 2: UUID.randomUUID().toString();
    }

    // 2) Idempotency check 
    if (processed.existsByHandlerAndEventId("submitted", eventId)) {
      return;
    }

    // 3) Payments update
    List<Payment> list = paymentRepo.findAllByBatchId(UUID.fromString(evt.getBatchId()));
    var now = OffsetDateTime.now();
    for (var p : list) {
      p.setState(com.payments.orch.domain.PaymentState.SUBMITTED);
      p.setUpdatedAt(now);
    }
    paymentRepo.saveAll(list);

    // 4) ProcessedEvent save with NON-NULL eventId
    processed.save(ProcessedEvent.builder()
        .handler("submitted")
        .eventId(eventId)      //
        .processedAt(now)
        .build());
  }

}
