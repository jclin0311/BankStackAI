package com.events.billpay;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Pain002Message(
        UUID paymentId,            // specific bill payment
        UUID batchId,              // kis batch ke andar tha
        String originalEndToEndId, // pain.001 / original reference (optional)
        String statusCode,         // e.g. "ACTC", "RJCT"
        String statusDescription,  // human readable
        boolean success,           // convenience flag → true = posted, false = failed
        String reason,             // failure/success detail summary
        OffsetDateTime receivedAt  // when we got this pain.002
) {
}
