package com.events.billpay;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BillBatchReadyEvent(
        UUID batchId,
        int lineCount,             // number of bill payments in this batch
        BigDecimal totalAmount,    // optional but useful for monitoring
        LocalDate cutoffDate,      // for which processing date / window
        String currency,           // e.g. "CAD"
        OffsetDateTime createdAt   // when batch was finalized
) {
}
