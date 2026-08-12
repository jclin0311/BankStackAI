package com.bankstack.mcpclient.api;

import com.commons.dto.ToolExecutionResult;

import java.util.Map;

public record GatewayChatResponse(
        String answer,
        boolean explicitConfirmationDetected,
        String route,
        String toolName,
        Map<String, Object> toolArguments,
        ToolExecutionResult toolResult,
        boolean workflowExecuted,
        String workflowType,
        String workflowPlanSummary,
        int workflowStepsExecuted,
        boolean workflowFallbackUsed
) {
    public GatewayChatResponse(String answer,
                               boolean explicitConfirmationDetected) {
        this(answer, explicitConfirmationDetected, null, null, null, null, false, null, null, 0, false);
    }

    public GatewayChatResponse(String answer,
                               boolean explicitConfirmationDetected,
                               String route) {
        this(answer, explicitConfirmationDetected, route, null, null, null, false, null, null, 0, false);
    }

    public GatewayChatResponse(String answer,
                               boolean explicitConfirmationDetected,
                               String route,
                               String toolName,
                               Map<String, Object> toolArguments,
                               ToolExecutionResult toolResult) {
        this(answer, explicitConfirmationDetected, route, toolName, toolArguments, toolResult, false, null, null, 0, false);
    }
}
