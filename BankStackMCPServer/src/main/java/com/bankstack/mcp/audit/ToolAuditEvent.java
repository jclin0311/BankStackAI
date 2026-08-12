package com.bankstack.mcp.audit;

import com.bankstack.mcp.safety.ToolAccessRole;

import java.time.OffsetDateTime;

public record ToolAuditEvent(
        OffsetDateTime timestamp,
        String toolName,
        String actorId,
        String subject,
        String customerExternalId,
        ToolAccessRole actorRole,
        String correlationId,
        boolean confirmationPresent,
        boolean otpVerified,
        AuditOutcome outcome,
        String requestSummary,
        String resultSummary,
        String errorMessage
) {}