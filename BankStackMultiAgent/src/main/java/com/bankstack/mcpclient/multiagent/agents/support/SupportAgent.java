package com.bankstack.mcpclient.multiagent.agents.support;

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
public class SupportAgent extends AbstractToolBackedDomainAgent {

    public SupportAgent(ScopedToolRegistry scopedToolRegistry,
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
        return AgentType.SUPPORT;
    }

    @Override
    public String agentName() {
        return "SupportAgent";
    }

    @Override
    protected String selectTool(AgentRequest request) {
        return "searchPolicyDocuments";
    }
}
