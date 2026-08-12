package com.commons.exception;

/**
 * Shared guardrail exception for AI gateway / workflow safety checks.
 *
 * Applications throw this from their guardrail services.
 * The shared GlobalExceptionHandler maps it to the common ErrorResponse envelope.
 */
public class GuardrailViolationException extends RuntimeException {

    private final String checkName;
    private final String riskLevel;

    public GuardrailViolationException(String checkName, String riskLevel, String message) {
        super(message);
        this.checkName = checkName;
        this.riskLevel = riskLevel;
    }

    public String checkName() {
        return checkName;
    }

    public String riskLevel() {
        return riskLevel;
    }
}
