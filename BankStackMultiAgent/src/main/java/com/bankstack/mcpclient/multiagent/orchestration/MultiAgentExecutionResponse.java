package com.bankstack.mcpclient.multiagent.orchestration;

import com.bankstack.mcpclient.multiagent.agents.AgentExecutionResult;
import com.bankstack.mcpclient.multiagent.router.RoutingPlan;

import java.util.List;

public record MultiAgentExecutionResponse(
        String answer,
        String mode,
        boolean multiDomain,
        int tasksPlanned,
        int tasksExecuted,
        boolean confirmationRequired,
        RoutingPlan routingPlan,
        List<AgentExecutionResult> agentResults
) {}
