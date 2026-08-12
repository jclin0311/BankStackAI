package com.events.billpay;

import java.util.UUID;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BillPayEnqueued {
  private String eventId;
  private UUID paymentId;
  private UUID batchId;
  private String correlationId;
  private String occurredAt;
  private String schemaVersion;
  private String channel;
}
