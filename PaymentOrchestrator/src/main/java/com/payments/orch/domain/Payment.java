package com.payments.orch.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;



@Entity
@Table(name="payments",
  uniqueConstraints = {
    @UniqueConstraint(name="uk_idempotency_key", columnNames="idempotency_key")
  },
  indexes = {
    @Index(name="idx_batch_id", columnList="batch_id"),
    @Index(name="idx_state", columnList="state")
  }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Payment {

  @Id
  @Column(name="payment_id", nullable=false, updatable=false)
  private UUID paymentId; // UUID generated per request

  @Enumerated(EnumType.STRING)
  @Column(nullable=false, length=20)
  private PaymentState state; // Single source of truth for lifecycle

  @Column(name="debtor_account_id", nullable=false, length=36)
  private UUID debtorAccountId; // Who pays

  @Column(name="biller_ref_number", nullable=false, length=64)
  private String billerRefNumber; // For BillPay; for non-BillPay channels you can leave null or move to details_json

  @Column(name="invoice_reference", nullable=false, length=128)
  private String invoiceReference; // Business reference, helps duplicate guard/reporting

  @Column(name="execution_date", nullable=false)
  private LocalDate executionDate; // Today or future; cutoff rules handled elsewhere

  @Column(name="amount_value", precision=18, scale=2, nullable=false)
  private BigDecimal amountValue; // Decimal amount in minor precision

  @Column(name="amount_ccy", length=3, nullable=false)
  private String amountCcy; // ISO 4217 (e.g., CAD)

  @Column(name="batch_id")
  private UUID batchId; // Set when Worker enqueues into a batch (BillPay/EFT)

  @Column(name="external_status_code")
  private String externalStatusCode; // e.g., pain.002 codes or RTR reason codes

  @Column(name="reason")
  private String reason; // Human-friendly short reason on failure/success

  @Column(name="idempotency_key", length=80, nullable=false)
  private String idempotencyKey; // Prevents duplicate intents for >= 24h

  
  @Column(name="created_at", nullable=false)
  private OffsetDateTime createdAt;

  @Column(name="updated_at", nullable=false)
  private OffsetDateTime updatedAt;
}
