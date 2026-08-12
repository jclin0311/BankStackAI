package com.bankstack.mcpclient.multiagent.agents.account;

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
public class AccountAgent extends AbstractToolBackedDomainAgent {

    public AccountAgent(ScopedToolRegistry scopedToolRegistry,
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
        return AgentType.ACCOUNT;
    }

    @Override
    public String agentName() {
        return "AccountAgent";
    }

    @Override
    protected String selectTool(AgentRequest request) {
        String intent = request.task().intent();
        if ("CHECK_ACCOUNT_BALANCE".equals(intent)) {
            return "getAccountBalance";
        }
        if ("GET_TRANSACTIONS".equals(intent)) {
            return "getTransactions";
        }
        if ("GET_CUSTOMER_PROFILE".equals(intent)) {
            return "getCustomerProfile";
        }
        return request.task().suggestedToolName();
    }
}
