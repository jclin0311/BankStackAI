package com.bankstack.mcp.tools.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BillPayPreparationResponse(
        String confirmationToken,
        String toolName,
        UUID debtorAccountId,
        String billerReferenceNumber,
        String invoiceReference,
        String executionDate,
        BigDecimal amount,
        String currency,
        String note,
        OffsetDateTime expiresAt,
        String message
) {}