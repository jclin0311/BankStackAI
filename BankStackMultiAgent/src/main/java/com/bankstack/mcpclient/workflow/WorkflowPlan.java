package com.bankstack.mcpclient.workflow;

import java.util.List;

public record WorkflowPlan(
        WorkflowType type,
        String name,
        String summary,
        boolean requiresConfirmation,
        List<WorkflowStep> steps
) {
}
