package com.events.billpay;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BillpayStatusEvent(
		UUID eventId,
        UUID paymentId,            // individual payment
        UUID batchId,              // belongs to which batch
        String status,             // e.g. "POSTED" / "FAILED"
        String reason,             // reason text pulled from pain.002
        OffsetDateTime updatedAt   // when status changed
) {}
