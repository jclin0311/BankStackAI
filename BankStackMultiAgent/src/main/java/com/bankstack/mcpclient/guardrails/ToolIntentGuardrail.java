package com.bankstack.mcpclient.guardrails;

import com.commons.exception.GuardrailViolationException;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Phase 8 checkpoint 2: intent guardrail.
 *
 * This verifies that the selected tool still matches the user's banking intent.
 * It prevents dangerous mismatches such as using a read-only/policy tool for a
 * payment execution request.
 */
@Component
public class ToolIntentGuardrail {

    private final IntentValidationGuard intentValidationGuard;

    public ToolIntentGuardrail(IntentValidationGuard intentValidationGuard) {
        this.intentValidationGuard = intentValidationGuard;
    }

    public GuardrailDecision validate(String toolName,
                                      String message,
                                      boolean explicitConfirmation,
                                      Map<String, Object> arguments) {
        GuardrailDecision decision = intentValidationGuard.evaluate(
                toolName,
                message,
                explicitConfirmation,
                arguments
        );

        if (!decision.allowed()) {
            throw new GuardrailViolationException(
                    decision.checkName(),
                    decision.riskLevel(),
                    decision.reason()
            );
        }

        return decision;
    }
}
