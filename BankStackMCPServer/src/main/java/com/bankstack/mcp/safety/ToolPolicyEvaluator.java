package com.bankstack.mcp.safety;

import com.commons.exception.ToolAccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class ToolPolicyEvaluator {

    public void evaluate(ToolExecutionPolicy policy, ToolInvocationContext context) {
        if (!policy.allowedRoles().contains(context.actorRole())) {
            throw new ToolAccessDeniedException(
                    "Tool '%s' is not allowed for role '%s'."
                            .formatted(policy.toolName(), context.actorRole())
            );
        }

        if (policy.confirmationRequired() && !context.confirmationPresent()) {
            throw new ToolAccessDeniedException(
                    "Tool '%s' requires explicit confirmation before execution."
                            .formatted(policy.toolName())
            );
        }

        if (policy.otpRequired() && !context.otpVerified()) {
            throw new ToolAccessDeniedException(
                    "Tool '%s' requires OTP verification before execution."
                            .formatted(policy.toolName())
            );
        }
    }
}
