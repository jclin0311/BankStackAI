package com.bankstack.mcpclient.guardrails;

import com.commons.exception.GuardrailViolationException;

import org.springframework.stereotype.Component;

/**
 * Phase 8 checkpoint 1: input guardrail.
 *
 * This is intentionally limited to conversation safety. It does not validate
 * actor roles, customer ownership, or banking authorization. Those checks are
 * enforced by BankStackMCPServer.
 */
@Component
public class InputGuardrail {

    private final PromptInjectionGuard promptInjectionGuard;

    public InputGuardrail(PromptInjectionGuard promptInjectionGuard) {
        this.promptInjectionGuard = promptInjectionGuard;
    }

    public GuardrailDecision validate(String message) {
        GuardrailDecision decision = promptInjectionGuard.evaluate(message);
        enforce(decision);
        return decision;
    }

    private void enforce(GuardrailDecision decision) {
        if (decision.allowed()) {
            return;
        }

        throw new GuardrailViolationException(
                decision.checkName(),
                decision.riskLevel(),
                decision.reason()
        );
    }
}
