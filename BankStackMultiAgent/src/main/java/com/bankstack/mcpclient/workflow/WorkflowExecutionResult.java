package com.bankstack.mcpclient.workflow;

public record WorkflowExecutionResult(
        String answer,
        WorkflowType workflowType,
        String planSummary,
        int stepsExecuted,
        boolean fallbackUsed,
        boolean waitingForConfirmation
) {
}
