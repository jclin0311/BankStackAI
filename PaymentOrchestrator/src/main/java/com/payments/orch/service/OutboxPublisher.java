package com.payments.orch.service;

import com.payments.orch.domain.Outbox;
import com.payments.orch.repo.OutboxRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

  private final OutboxRepo outboxRepo;
  private final KafkaTemplate<String,String> kafka;

  @Scheduled(fixedDelayString = "${outbox.publish.fixedDelayMs:5000}")
  @Transactional
  public void publish() {
    List<Outbox> batch = outboxRepo.findTop200ByStateOrderByIdAsc("PENDING");
    for (var row: batch) {
      try {
        kafka.send(row.getTopic(), row.getKey().toString(), row.getPayloadJson()).get();
        row.setState("PUBLISHED");
      } catch (Exception e) {
        log.error("Outbox publish failed id={} topic={} key={}", row.getId(), row.getTopic(), row.getKey(), e);
        row.setState("FAILED");
      }
      row.setUpdatedAt(OffsetDateTime.now());
    }
    outboxRepo.saveAll(batch);
  }
}
