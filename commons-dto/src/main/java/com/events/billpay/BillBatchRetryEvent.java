package com.events.billpay;

import java.time.OffsetDateTime;
import java.util.UUID;
public record BillBatchRetryEvent(
	    UUID batchId,
	    int attemptNumber,
	    String reason,
	    OffsetDateTime requestedAt
	)
 {

	// 
    public BillBatchRetryEvent(UUID batchId, String reason) {
        this(batchId, 0,reason, OffsetDateTime.now());
    }
}