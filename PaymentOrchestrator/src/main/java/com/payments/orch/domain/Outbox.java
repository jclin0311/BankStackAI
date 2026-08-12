package com.payments.orch.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Transactional outbox entry. Persist inside the same DB transaction
 * as your business state change; a background publisher will ship it to Kafka.
 */
@Entity @Table(name="outbox")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Outbox {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false, length=120)
  private String topic;      // Kafka topic name

  @Column(nullable=false, length=120)
  private UUID key;        // Kafka message key (partitioning, ordering)

  @Lob @Column(nullable=false)
  private String payloadJson; // Serialized event JSON (schema owned by your service)

  @Column(nullable=false, length=20)
  private String state; // PENDING → PUBLISHED (or FAILED if publish attempt crashes)

  @Column(name="created_at", nullable=false)
  private OffsetDateTime createdAt;

  @Column(name="updated_at", nullable=false)
  private OffsetDateTime updatedAt;
}
