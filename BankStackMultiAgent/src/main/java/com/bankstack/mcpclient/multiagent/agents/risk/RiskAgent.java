package com.bankstack.mcpclient.multiagent.agents.risk;

import com.bankstack.mcpclient.gateway.McpToolExecutionService;
import com.bankstack.mcpclient.gateway.ToolArgumentContractValidator;
import com.bankstack.mcpclient.gateway.ToolArgumentExtractionService;
import com.bankstack.mcpclient.gateway.ToolArgumentNormalizer;
import com.bankstack.mcpclient.gateway.ToolGatewayPolicyService;
import com.bankstack.mcpclient.gateway.ToolResponsePresenter;
import com.bankstack.mcpclient.gateway.ConversationContextStore;
import com.bankstack.mcpclient.memory.ActionMemoryService;
import com.bankstack.mcpclient.guardrails.WorkflowGuardrailService;
import com.bankstack.mcpclient.multiagent.agents.AbstractToolBackedDomainAgent;
import com.bankstack.mcpclient.multiagent.agents.AgentRequest;
import com.bankstack.mcpclient.multiagent.audit.MultiAgentAuditService;
import com.bankstack.mcpclient.multiagent.router.AgentType;
import com.bankstack.mcpclient.multiagent.tools.ScopedToolRegistry;
import org.springframework.stereotype.Component;

@Component
public class RiskAgent extends AbstractToolBackedDomainAgent {

    public RiskAgent(ScopedToolRegistry scopedToolRegistry,
                     ToolArgumentExtractionService extractionService,
                     ToolArgumentNormalizer normalizer,
                     ToolArgumentContractValidator contractValidator,
                     WorkflowGuardrailService workflowGuardrailService,
                     McpToolExecutionService mcpToolExecutionService,
                     ToolResponsePresenter toolResponsePresenter,
                     ConversationContextStore contextStore,
                        ActionMemoryService actionMemoryService,
                     ToolGatewayPolicyService policyService,
                     MultiAgentAuditService auditService) {
        super(scopedToolRegistry, extractionService, normalizer, contractValidator, workflowGuardrailService,
                mcpToolExecutionService, toolResponsePresenter, contextStore, actionMemoryService, policyService, auditService);
    }

    @Override
    public AgentType supports() {
        return AgentType.RISK;
    }

    @Override
    public String agentName() {
        return "RiskAgent";
    }

    @Override
    protected String selectTool(AgentRequest request) {
        if ("REVIEW_SUSPICIOUS_ACTIVITY".equals(request.task().intent())) {
            return "assessTransactionRisk";
        }
        return null;
    }

    @Override
    protected String noToolMessage(AgentRequest request) {
        return "RiskAgent identified a risk-related request, but could not map it to the governed risk assessment tool. The safe response is to route the user to a governed support or fraud-review flow rather than guessing.";
    }
}
