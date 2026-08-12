package com.billpay.worker.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "batch_lines")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class BatchLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false, length = 64)
    private UUID batchId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

  /*  @Column(name = "checksum", length = 128)
    private String checksum;*/

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}