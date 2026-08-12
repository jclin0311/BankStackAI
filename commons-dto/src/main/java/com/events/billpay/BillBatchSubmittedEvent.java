package com.events.billpay;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BillBatchSubmittedEvent(
        UUID batchId,               // batch that got uploaded to Central1
        String centralReference,    // reference returned by Central1 / SFTP / API
        OffsetDateTime submittedAt  // optional but great for audit
) {
    public BillBatchSubmittedEvent(UUID batchId, String centralReference) {
        this(batchId, centralReference, OffsetDateTime.now());
    }
}
