package com.bankstack.mcpclient.multiagent.orchestration;

import com.bankstack.mcpclient.multiagent.agents.AgentExecutionResult;
import com.bankstack.mcpclient.multiagent.agents.AgentExecutionStatus;
import com.bankstack.mcpclient.multiagent.router.RoutingPlan;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class MultiAgentResponseComposer {

    public String compose(RoutingPlan plan, List<AgentExecutionResult> results) {
        if (results == null || results.isEmpty()) {
            return "I could not route this request to a banking specialist. Please rephrase your request.";
        }

        List<String> lines = new ArrayList<>();

        if (plan != null && plan.multiDomain()) {
            lines.add("I detected a multi-domain banking request and routed it to the right specialists.");
            lines.add("");
        }

        int index = 1;
        for (AgentExecutionResult result : results) {
            lines.add(index + ". " + label(result) + ": " + clean(result.answer()));
            index++;
        }

        Set<String> missing = new LinkedHashSet<>();
        boolean confirmationRequired = false;
        for (AgentExecutionResult result : results) {
            if (result.missingFields() != null) {
                missing.addAll(result.missingFields());
            }
            confirmationRequired = confirmationRequired || result.confirmationRequired() || result.status() == AgentExecutionStatus.PREPARED;
        }

        if (!missing.isEmpty()) {
            lines.add("");
            lines.add("To continue, please provide: " + String.join(", ", missing) + ".");
        }

        if (confirmationRequired) {
            lines.add("");
            lines.add("No money has been moved yet. Please explicitly confirm if you want to execute the prepared payment.");
        }

        return String.join("\n", lines).trim();
    }

    private String label(AgentExecutionResult result) {
        if (result == null || result.agentType() == null) {
            return "Banking specialist";
        }
        return switch (result.agentType()) {
            case ACCOUNT -> "Account specialist";
            case PAYMENT -> "Payment operations specialist";
            case SUPPORT -> "Support specialist";
            case RISK -> "Risk analyst";
        };
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return "No response produced.";
        }
        return value.trim();
    }
}
