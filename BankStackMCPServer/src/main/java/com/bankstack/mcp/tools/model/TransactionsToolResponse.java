package com.bankstack.mcp.tools.model;

import java.util.List;
import java.util.UUID;

import com.bankstack.mcp.dto.TransactionItem;

public record TransactionsToolResponse(
        UUID accountId,
        int count,
        int limit,
        int offset,
        List<TransactionItem> transactions,
        String message
) {}