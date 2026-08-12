package com.bankstack.mcpclient.multiagent.router;

public record RoutedTask(
        String taskId,
        AgentType agentType,
        String intent,
        String originalUserMessage,
        boolean toolCandidate,
        String suggestedToolName,
        boolean executionAllowedByRouter,
        String reason
) {
}
