package com.bankstack.mcp.safety;

import java.util.Set;

public record ToolExecutionPolicy(
        String toolName,
        ToolCategory category,
        ToolRiskLevel riskLevel,
        Set<ToolAccessRole> allowedRoles,
        boolean confirmationRequired,
        boolean otpRequired
) {}