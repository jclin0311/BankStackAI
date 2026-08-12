package com.bankstack.mcp.security;

import com.bankstack.mcp.safety.ToolAccessRole;

public record CurrentActor(
        String actorId,
        String subject,
        String customerExternalId,
        ToolAccessRole role
) {}