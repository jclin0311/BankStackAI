package com.bankstack.mcp.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One ledger entry as handed to the agent.
 *
 * <p>Carries the type and timestamp so an answer can distinguish a hold from a posted
 * debit, and can say when something happened. There is deliberately no per-row status
 * message: repeating "retrieved successfully" on every row is noise to a model trying to
 * summarise the list, and the outcome is already reported once for the whole call.</p>
 */
public record TransactionItem(
        UUID transactionId,
        String type,
        String status,
        BigDecimal amount,
        String currency,
        String reason,
        BigDecimal balanceAfter,
        OffsetDateTime occurredAt
) {}
