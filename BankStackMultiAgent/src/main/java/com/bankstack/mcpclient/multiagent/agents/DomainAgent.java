package com.bankstack.mcpclient.multiagent.agents;

import com.bankstack.mcpclient.multiagent.router.AgentType;

public interface DomainAgent {
    AgentType supports();
    String agentName();
    AgentExecutionResult execute(AgentRequest request);
}
