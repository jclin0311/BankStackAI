package com.bankstack.mcp.confirmation;

import java.time.OffsetDateTime;
import java.util.Map;

public record PendingToolAction(
        String confirmationToken,
        String toolName,
        String actorId,
        Map<String, Object> payload,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt
) {}