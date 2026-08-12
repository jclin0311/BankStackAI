package com.bankstack.mcpclient.multiagent.tools;

import com.bankstack.mcpclient.multiagent.router.AgentType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ScopedToolRegistry {

    private final Map<AgentType, Set<String>> allowedTools = Map.of(
            AgentType.ACCOUNT, Set.of("getAccountBalance", "getTransactions", "getCustomerProfile"),
            AgentType.PAYMENT, Set.of("prepareBillPay", "confirmBillPay", "getPaymentStatus"),
            AgentType.SUPPORT, Set.of("searchPolicyDocuments"),
            AgentType.RISK, Set.of("assessTransactionRisk")
    );

    public boolean isAllowed(AgentType agentType, String toolName) {
        if (agentType == null || toolName == null || toolName.isBlank()) {
            return false;
        }
        return allowedTools.getOrDefault(agentType, Set.of()).contains(toolName);
    }

    public Set<String> allowedToolsFor(AgentType agentType) {
        return allowedTools.getOrDefault(agentType, Set.of());
    }
}
