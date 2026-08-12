package com.bankstack.mcp.tools.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FraudRiskAssessmentResponse(
        UUID accountId,
        BigDecimal amount,
        String payee,
        String channel,
        int riskScore,
        String riskLevel,
        List<String> reasons,
        String recommendedAction,
        String message
) {
}