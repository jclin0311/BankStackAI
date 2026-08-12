package com.bankstack.mcp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentView(
        UUID paymentId,
        String state,
        UUID debtorAccountId,
        String billerRefNumber,
        String invoiceReference,
        LocalDate executionDate,
        BigDecimal amountValue,
        String amountCcy,
        UUID batchId,
        String externalStatusCode,
        String reason,
        String idempotencyKey,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}