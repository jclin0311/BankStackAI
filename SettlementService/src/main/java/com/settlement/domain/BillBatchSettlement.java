package com.settlement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bill_batch_settlement")
@Getter
@Setter
public class BillBatchSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID batchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;
    
    
    private int retryCount;   // default 0


    private String pain001FileName;

    private String centralReference;

    private String lastError;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
