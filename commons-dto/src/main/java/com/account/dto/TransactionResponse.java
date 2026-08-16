package com.account.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A ledger entry as returned by AccountService.
 *
 * <p>{@code type} and {@code occurredAt} are what make an entry identifiable. A single
 * bill payment writes three rows of the same amount — HOLD_PLACED, HOLD_RELEASED and
 * DEBIT — so without the type a caller sees three indistinguishable figures and cannot
 * tell a reserved amount from money that actually left the account.</p>
 */
public record TransactionResponse(
        UUID id,
        String type,
        String status,
        BigDecimal amount,
        String currency,
        String reason,
        BigDecimal balanceAfter,
        OffsetDateTime occurredAt
) {}
