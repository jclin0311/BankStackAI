package com.payments.orch.repo;

import com.payments.orch.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepo extends JpaRepository<ProcessedEvent, Long> {
  boolean existsByHandlerAndEventId(String handler, String eventId);
}
