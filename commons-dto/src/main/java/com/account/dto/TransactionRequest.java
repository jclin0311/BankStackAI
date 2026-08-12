package com.account.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record TransactionRequest(
        @NotNull UUID accountId,
        @NotNull BigDecimal amount,
        @NotNull String currency,
        @NotNull String type,            // CREDIT | DEBIT | HOLD_PLACED | HOLD_RELEASED
        String reason,
        BigDecimal balanceAfter,         // only for posting events
        OffsetDateTime occurredAt        // server time
) {}
