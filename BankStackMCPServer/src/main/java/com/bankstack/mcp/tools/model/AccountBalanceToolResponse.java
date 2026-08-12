package com.bankstack.mcp.tools.model;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountBalanceToolResponse(
        UUID accountId,
        BigDecimal availableBalance,
        BigDecimal currentBalance,
        String currency,
        String message
) {}