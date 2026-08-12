package com.bankstack.mcpclient.multiagent.agents;

import com.commons.dto.ToolExecutionResult;
import com.bankstack.mcpclient.multiagent.router.AgentType;

import java.util.List;
import java.util.Map;

public record AgentExecutionResult(
        AgentType agentType,
        String agentName,
        String taskId,
        String intent,
        String selectedTool,
        AgentExecutionStatus status,
        String answer,
        List<String> missingFields,
        boolean confirmationRequired,
        String confirmationToken,
        Map<String, Object> arguments,
        ToolExecutionResult toolResult,
        List<String> warnings
) {
    public static AgentExecutionResult skipped(AgentType agentType,
                                               String agentName,
                                               String taskId,
                                               String intent,
                                               String reason) {
        return new AgentExecutionResult(
                agentType,
                agentName,
                taskId,
                intent,
                null,
                AgentExecutionStatus.SKIPPED,
                reason,
                List.of(),
                false,
                null,
                Map.of(),
                null,
                List.of(reason)
        );
    }

    public boolean needsInput() {
        return status == AgentExecutionStatus.NEEDS_INPUT;
    }

    public boolean prepared() {
        return status == AgentExecutionStatus.PREPARED;
    }

    public boolean failedOrBlocked() {
        return status == AgentExecutionStatus.FAILED || status == AgentExecutionStatus.BLOCKED;
    }
}
