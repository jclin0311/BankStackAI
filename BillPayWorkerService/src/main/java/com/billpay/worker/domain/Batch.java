package com.billpay.worker.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "batches")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Batch {

    @Id
    @Column(name = "batch_id", length = 64)
    private UUID batchId;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // OPEN, READY_EMITTED

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}