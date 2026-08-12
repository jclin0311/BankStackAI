package com.bankstack.rag.model;

import java.time.Instant;

public record DocumentRef(
        String docId,
        String docType,
        String version,
        String title,
        Instant effectiveFrom,
        Instant effectiveTo
) {
}