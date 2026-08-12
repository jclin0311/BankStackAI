package com.events.billpay;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BillPayRequested {
  private String eventId;
  private UUID  paymentId;
  private String correlationId;
  private UUID debtorAccountId;
  private String billerRefNumber;
  private String invoiceReference;
  private String executionDate;   // yyyy-MM-dd
  private BigDecimal amountValue;
  private String amountCcy;
  private String occurredAt;      // ISO
  private String schemaVersion;   // "1"
  private String channel;         // "billpay"
}
