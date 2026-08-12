package com.bankstack.mcpclient.workflow;

public record WorkflowStep(
        WorkflowStepType type,
        String name,
        String toolName,
        boolean retryable,
        boolean highRisk
) {
    public static WorkflowStep standard(WorkflowStepType type, String name) {
        return new WorkflowStep(type, name, null, false, false);
    }

    public static WorkflowStep tool(String name, String toolName, boolean retryable, boolean highRisk) {
        return new WorkflowStep(WorkflowStepType.TOOL, name, toolName, retryable, highRisk);
    }
}
