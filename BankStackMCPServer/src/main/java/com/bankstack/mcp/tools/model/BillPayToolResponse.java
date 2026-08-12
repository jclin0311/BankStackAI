package com.bankstack.mcp.tools.model;

import java.math.BigDecimal;
import java.util.UUID;

public record BillPayToolResponse(
        UUID paymentId,
        String state,
        String statusUrl,
        UUID debtorAccountId,
        String billerReferenceNumber,
        String invoiceReference,
        BigDecimal amount,
        String currency,
        String executionDate,
        String idempotencyKey,
        String message
) {}