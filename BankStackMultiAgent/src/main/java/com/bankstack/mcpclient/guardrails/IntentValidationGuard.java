package com.bankstack.mcpclient.guardrails;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

@Component
public class IntentValidationGuard {

    public GuardrailDecision evaluate(String toolName,
                                      String message,
                                      boolean explicitConfirmation,
                                      Map<String, Object> arguments) {

        if (toolName == null || toolName.isBlank()) {
            return GuardrailDecision.deny(
                    "INTENT_VALIDATION",
                    "HIGH",
                    "No approved tool was selected for this request."
            );
        }

        String normalized = message == null
                ? ""
                : message.toLowerCase(Locale.ROOT);

        if ("confirmBillPay".equals(toolName) && !explicitConfirmation) {
            return GuardrailDecision.deny(
                    "INTENT_VALIDATION",
                    "HIGH",
                    "Payment execution requires explicit user confirmation."
            );
        }

        if (isReadOnlyTool(toolName) && containsPaymentExecutionIntent(normalized)) {
            return GuardrailDecision.deny(
                    "INTENT_VALIDATION",
                    "MEDIUM",
                    "The selected read-only tool does not match the user's payment execution intent."
            );
        }

        if ("searchPolicyDocuments".equals(toolName) && containsBankingActionIntent(normalized)) {
            return GuardrailDecision.deny(
                    "INTENT_VALIDATION",
                    "MEDIUM",
                    "Policy search cannot be used to execute banking actions."
            );
        }

        if ("prepareBillPay".equals(toolName) || "confirmBillPay".equals(toolName)) {
            BigDecimal amount = extractAmount(arguments);
            if (amount != null && amount.compareTo(new BigDecimal("5000.00")) > 0) {
                return GuardrailDecision.deny(
                        "TRANSACTION_LIMIT_PRECHECK",
                        "HIGH",
                        "This payment amount exceeds the AI gateway pre-check limit. MCP/server-side limits remain the final enforcement boundary."
                );
            }
        }

        return GuardrailDecision.allow("INTENT_VALIDATION");
    }

    private boolean isReadOnlyTool(String toolName) {
        return "getAccountBalance".equals(toolName)
                || "getTransactions".equals(toolName)
                || "getCustomerProfile".equals(toolName)
                || "getPaymentStatus".equals(toolName);
    }

    private boolean containsPaymentExecutionIntent(String normalized) {
        return normalized.contains("pay ")
                || normalized.contains("make payment")
                || normalized.contains("send money")
                || normalized.contains("transfer money")
                || normalized.contains("execute payment")
                || normalized.contains("confirm payment");
    }

    private boolean containsBankingActionIntent(String normalized) {
        return normalized.contains("pay ")
                || normalized.contains("transfer")
                || normalized.contains("debit")
                || normalized.contains("credit")
                || normalized.contains("confirm bill")
                || normalized.contains("execute");
    }

    private BigDecimal extractAmount(Map<String, Object> arguments) {
        if (arguments == null) {
            return null;
        }

        Object raw = arguments.get("amount");
        if (raw == null) {
            raw = arguments.get("paymentAmount");
        }
        if (raw == null) {
            return null;
        }

        try {
            return new BigDecimal(raw.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
