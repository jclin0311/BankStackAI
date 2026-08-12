package com.bankstack.mcpclient.multiagent.router;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class MultiAgentRouter {

    public RoutingPlan route(String message) {
        String text = normalize(message);
        List<RoutedTask> tasks = new ArrayList<>();

        if (looksLikeAccountBalanceRequest(text)) {
            tasks.add(task(
                    AgentType.ACCOUNT,
                    "CHECK_ACCOUNT_BALANCE",
                    message,
                    true,
                    "getAccountBalance",
                    "Balance inquiry belongs to Account Agent."
            ));
        }

        if (looksLikeTransactionRequest(text)) {
            tasks.add(task(
                    AgentType.ACCOUNT,
                    "GET_TRANSACTIONS",
                    message,
                    true,
                    "getTransactions",
                    "Transaction lookup belongs to Account Agent."
            ));
        }

        if (looksLikeBillPaymentRequest(text)) {
            tasks.add(task(
                    AgentType.PAYMENT,
                    "PREPARE_BILL_PAYMENT",
                    message,
                    true,
                    "prepareBillPay",
                    "Bill payment must be handled by Payment Agent and requires confirmation before execution."
            ));
        }

        if (looksLikePaymentStatusRequest(text)) {
            tasks.add(task(
                    AgentType.PAYMENT,
                    "GET_PAYMENT_STATUS",
                    message,
                    true,
                    "getPaymentStatus",
                    "Payment status lookup belongs to Payment Agent."
            ));
        }

        if (looksLikeRiskRequest(text)) {
            tasks.add(task(
                    AgentType.RISK,
                    "REVIEW_SUSPICIOUS_ACTIVITY",
                    message,
                    true,
                    "assessTransactionRisk",
                    "Suspicious activity assessment belongs to Risk Agent using the governed read-only risk tool."
            ));
        }

        if (looksLikeSupportOrPolicyRequest(text)) {
            tasks.add(task(
                    AgentType.SUPPORT,
                    "ANSWER_POLICY_OR_SUPPORT_QUESTION",
                    message,
                    true,
                    "searchPolicyDocuments",
                    "Policy/support explanation belongs to Support Agent with scoped RAG."
            ));
        }

        if (tasks.isEmpty()) {
            tasks.add(task(
                    AgentType.SUPPORT,
                    "GENERAL_SUPPORT",
                    message,
                    false,
                    null,
                    "No precise banking domain detected. Route to Support Agent for safe clarification."
            ));
        }

        return RoutingPlan.of(tasks);
    }

    private RoutedTask task(AgentType agentType,
                            String intent,
                            String message,
                            boolean toolCandidate,
                            String suggestedToolName,
                            String reason) {
        return new RoutedTask(
                UUID.randomUUID().toString(),
                agentType,
                intent,
                message,
                toolCandidate,
                suggestedToolName,
                false,
                reason
        );
    }

    private String normalize(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        return message.toLowerCase(Locale.ROOT).trim();
    }

    private boolean looksLikeAccountBalanceRequest(String text) {
        return containsAny(text, "balance", "available funds", "available balance", "how much money");
    }

    private boolean looksLikeTransactionRequest(String text) {
        return containsAny(text, "transaction", "transactions", "last purchase", "recent activity", "statement");
    }

    private boolean looksLikeBillPaymentRequest(String text) {
        return containsAny(text, "pay", "bill", "biller", "hydro", "rogers", "credit card payment")
                && containsAny(text, "pay", "payment", "bill");
    }

    private boolean looksLikePaymentStatusRequest(String text) {
        return containsAny(text, "payment status", "status of payment", "did my payment", "payment go through");
    }

    private boolean looksLikeRiskRequest(String text) {
        return containsAny(text, "suspicious", "fraud", "risky", "risk", "declined", "blocked", "unauthorized");
    }

    private boolean looksLikeSupportOrPolicyRequest(String text) {
        return containsAny(text, "policy", "why", "explain", "help", "support", "what happens", "allowed");
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
