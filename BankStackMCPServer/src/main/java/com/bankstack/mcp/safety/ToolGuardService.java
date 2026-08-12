package com.bankstack.mcp.safety;

import org.springframework.stereotype.Service;

@Service
public class ToolGuardService {

    private final ToolExecutionPolicies toolExecutionPolicies;
    private final ToolPolicyEvaluator toolPolicyEvaluator;

    public ToolGuardService(ToolExecutionPolicies toolExecutionPolicies,
                            ToolPolicyEvaluator toolPolicyEvaluator) {
        this.toolExecutionPolicies = toolExecutionPolicies;
        this.toolPolicyEvaluator = toolPolicyEvaluator;
    }

    public void check(String toolName, ToolInvocationContext context) {
        ToolExecutionPolicy policy = toolExecutionPolicies.getPolicy(toolName);
        toolPolicyEvaluator.evaluate(policy, context);
    }

    public ToolExecutionPolicy getPolicy(String toolName) {
        return toolExecutionPolicies.getPolicy(toolName);
    }
}