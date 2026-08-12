package com.bankstack.mcp.dto;

import java.util.List;

public record PolicySearchResponse(
        String answer,
        String taskType,
        boolean fullyVerified,
        List<Object> citationChecks,
        String renderedContext
) {
}
