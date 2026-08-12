package com.bankstack.mcpclient.workflow;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkflowResponseComposer {

    public String compose(WorkflowPlan plan,
                          List<String> toolOutputs,
                          boolean waitingForConfirmation,
                          boolean fallbackUsed) {

        StringBuilder response = new StringBuilder();
        response.append("Workflow: ").append(plan.name()).append("\n\n");
        response.append("Plan: ").append(plan.summary()).append("\n\n");

        if (toolOutputs == null || toolOutputs.isEmpty()) {
            response.append("No tool output was produced yet.");
        } else {
            response.append("Outcome:\n");
            for (String output : toolOutputs) {
                if (output != null && !output.isBlank()) {
                    response.append("- ").append(output.replace("\n", "\n  ")).append("\n");
                }
            }
        }

        if (waitingForConfirmation) {
            response.append("\nExplicit confirmation is required before the high-risk execution step can continue.");
        }

        if (fallbackUsed) {
            response.append("\nA retry/fallback was used for one of the workflow steps.");
        }

        return response.toString().trim();
    }
}
