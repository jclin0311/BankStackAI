package com.bankstack.mcpclient.multiagent.orchestration;

import com.bankstack.mcpclient.multiagent.agents.DomainAgent;
import com.bankstack.mcpclient.multiagent.router.AgentType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class DomainAgentRegistry {

    private final Map<AgentType, DomainAgent> agents = new EnumMap<>(AgentType.class);

    public DomainAgentRegistry(List<DomainAgent> domainAgents) {
        for (DomainAgent agent : domainAgents) {
            agents.put(agent.supports(), agent);
        }
    }

    public DomainAgent get(AgentType agentType) {
        DomainAgent agent = agents.get(agentType);
        if (agent == null) {
            throw new IllegalStateException("No domain agent registered for: " + agentType);
        }
        return agent;
    }
}



