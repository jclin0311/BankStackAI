package com.account.model;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
    name = "account_transaction",
    indexes = {
        @Index(name = "idx_tx_account", columnList = "accountId"),
        @Index(name = "idx_tx_occurred", columnList = "occurredAt"),
        @Index(name = "idx_tx_type", columnList = "type"),
        @Index(name = "idx_tx_status", columnList = "status")
    },
    uniqueConstraints = {
        // Idempotency per account
        @UniqueConstraint(name = "uk_tx_account_idem", columnNames = {"accountId","requestFingerprint"})
    }
)
public class Transaction {

    /** DB key (stable, sortable). Keep Long if you like sequences; else use UUID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public identifier (safe to expose). */
    @Column(nullable = false, updatable = false, unique = true)
    private UUID transactionId;

    /** Which account this transaction belongs to. */
    @Column(nullable = false)
    private UUID accountId;

    /** CREDIT | DEBIT | HOLD_PLACED | HOLD_RELEASED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionType type;

    /** Business status: PENDING/POSTED/REVERSED/FAILED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionStatus status;

    /** Signed amount; for DEBIT we still store positive amount (business sign is in type). */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;


    @Column(length = 3, nullable = false)
    private String currency;

    @Column(length = 256)
    private String reason;

    /** For postings (DEBIT/CREDIT) only; null for holds. */
    @Column(precision = 19, scale = 2)
    private BigDecimal balanceAfter;


    @Column(nullable = false)
    private OffsetDateTime occurredAt;


    @Column(length = 100)
    private String requestFingerprint;

 
    /** System timestamps. */
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Version
    private Integer version;

    @PrePersist
    void prePersist() {
        if (transactionId == null) transactionId = UUID.randomUUID();
        if (occurredAt == null)    occurredAt = OffsetDateTime.now();
        if (createdAt == null)     createdAt = OffsetDateTime.now();
        if (updatedAt == null)     updatedAt = createdAt;
        if (status == null)        status = TransactionStatus.POSTED; // logging from AccountService is for posted events
        if (currency == null)      currency = "CAD";
        if (version == null)       version = 0;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
