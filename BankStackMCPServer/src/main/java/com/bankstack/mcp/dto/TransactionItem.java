package com.bankstack.mcp.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionItem(
        UUID transactionId,
        String status,
        BigDecimal amount,
        String reason,
        BigDecimal balanceAfter,
        String message
) {}