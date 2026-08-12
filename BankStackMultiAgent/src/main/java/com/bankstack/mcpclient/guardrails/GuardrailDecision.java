package com.bankstack.mcpclient.guardrails;

public record GuardrailDecision(
        boolean allowed,
        String checkName,
        String riskLevel,
        String reason
) {

    public static GuardrailDecision allow(String checkName) {
        return new GuardrailDecision(true, checkName, "LOW", "Allowed");
    }

    public static GuardrailDecision deny(String checkName, String riskLevel, String reason) {
        return new GuardrailDecision(false, checkName, riskLevel, reason);
    }
}
