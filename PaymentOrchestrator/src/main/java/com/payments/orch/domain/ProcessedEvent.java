package com.payments.orch.domain;


import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * De-duplication table for consumers.
 * Each consumer/handler writes (handler, eventId) after successful processing.
 * On replays, simply skip if exists → exactly-once side-effects.
 */
@Entity
@Table(name="processed_events",
  uniqueConstraints=@UniqueConstraint(name="uk_handler_event", columnNames={"handler","event_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProcessedEvent {

  @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false, length=80)
  private String handler; // e.g., "enqueued" | "submitted" | "status"

  @Column(name="event_id", nullable=false, length=120)
  private String eventId; // Event UUID

  @Column(name="processed_at", nullable=false)
  private OffsetDateTime processedAt;
}
