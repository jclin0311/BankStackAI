package com.bankstack.mcpclient.multiagent.audit;

import com.bankstack.mcpclient.multiagent.agents.AgentExecutionResult;
import com.bankstack.mcpclient.multiagent.router.RoutingPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MultiAgentAuditService {

    private static final Logger log = LoggerFactory.getLogger(MultiAgentAuditService.class);

    public void routerDecision(String sessionKey, RoutingPlan plan) {
        log.info("multiagent_router_decision sessionKey={} actor={} multiDomain={} taskCount={} summary={}",
                sessionKey,
                plan != null && plan.multiDomain(),
                plan == null ? 0 : plan.taskCount(),
                plan == null ? "" : plan.summary());
    }

    public void agentSelected(String sessionKey, String taskId, String agentName, String intent, String toolName) {
        log.info("multiagent_selected_agent sessionKey={} taskId={} agent={} intent={} tool={}",
                sessionKey, taskId, agentName, intent, toolName);
    }

    public void agentCompleted(String sessionKey, AgentExecutionResult result) {
        log.info("multiagent_agent_completed sessionKey={} taskId={} agent={} intent={} tool={} status={} confirmationRequired={} missingFields={}",
                sessionKey,
                result == null ? "" : result.taskId(),
                result == null ? "" : result.agentName(),
                result == null ? "" : result.intent(),
                result == null ? "" : result.selectedTool(),
                result == null ? "" : result.status(),
                result != null && result.confirmationRequired(),
                result == null ? "[]" : result.missingFields());
    }

    public void finalResponse(String sessionKey, String mode, String response) {
        log.info("multiagent_final_response sessionKey={} mode={} responsePreview={}",
                sessionKey, mode, preview(response));
    }

    private String preview(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.replaceAll("\\s+", " ").trim();
        return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 300);
    }
}
