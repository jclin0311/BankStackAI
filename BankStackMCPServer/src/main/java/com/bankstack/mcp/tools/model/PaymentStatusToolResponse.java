package com.bankstack.mcp.tools.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentStatusToolResponse(
        UUID paymentId,
        String state,
        UUID debtorAccountId,
        String billerReferenceNumber,
        String invoiceReference,
        LocalDate executionDate,
        BigDecimal amount,
        String currency,
        String externalStatusCode,
        String reason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String message
) {}