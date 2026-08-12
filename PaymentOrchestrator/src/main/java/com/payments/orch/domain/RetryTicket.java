package com.payments.orch.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Schedules/reschedules batch retries with jitter/backoff.
 * A lightweight scheduler picks due tickets and emits resubmit events.
 */
@Entity
@Table(name="retries",
  uniqueConstraints=@UniqueConstraint(name="pk_batch_attempt", columnNames={"batch_id","attempt"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RetryTicket {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
  private Long id;

  @Column(name="batch_id", nullable=false, length=64)
  private String batchId;

  @Column(nullable=false)
  private int attempt; // Current attempt number (0..N)

  @Column(name="next_attempt_at", nullable=false)
  private OffsetDateTime nextAttemptAt;

  @Column(name="backoff_ms", nullable=false)
  private long backoffMs; // Used for monitoring/analytics

  @Column(nullable=false, length=20)
  private String status; // SCHEDULED | SENT | CANCELED | DLQ

  @Column(length=256)
  private String reason; // Why the batch was scheduled for retry
}
