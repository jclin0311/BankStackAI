package com.bankstack.mcpclient.gateway;

import java.util.Map;

public record AgentDecision(

        String route,

        String toolName,

        Double confidence,

        String reason,

        Map<String, Object> toolArguments,

        String directResponse

) {

    public boolean isToolRoute() {
        return "TOOL".equalsIgnoreCase(route);
    }

    public boolean isDirectRoute() {
        return "DIRECT".equalsIgnoreCase(route);
    }

    public boolean isRefusalRoute() {
        return "REFUSE".equalsIgnoreCase(route);
    }

    public boolean isWorkflowRoute() {
        return "WORKFLOW".equalsIgnoreCase(route);
    }

    public boolean isConfident() {
        return confidence != null && confidence >= 0.60;
    }

    public Map<String, Object> safeToolArguments() {
        return toolArguments == null ? Map.of() : toolArguments;
    }
}