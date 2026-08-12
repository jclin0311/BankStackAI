package com.bankstack.mcpclient.gateway;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class DeterministicIntentRouter {

    public AgentDecision route(String message,
                               boolean explicitConfirmation,
                               ConversationContext context) {

        String value = normalize(message);

        /*
         * Explicit confirmation flow should remain deterministic.
         */
        if (explicitConfirmation
                && context != null
                && context.hasPreparedAction()) {

            return new AgentDecision(
                    "WORKFLOW",
                    "BILL_PAYMENT_EXECUTION",
                    1.0,
                    "explicit confirmation continues prepared bill-payment workflow",
                    Map.of(),
                    null
            );
        }

        /*
         * Awaiting-input flow is continued ONLY when the user's next message
         * actually looks like an answer to the missing field.
         *
         * If the user asks a new intent instead, do not hijack the conversation
         * back to the previous awaiting tool.
         */
        if (context != null && context.isAwaitingInput()) {
            if (looksLikeMissingFieldAnswer(message, context)) {
                return new AgentDecision(
                        "TOOL",
                        context.awaitingTool(),
                        1.0,
                        "continuing missing-input flow",
                        Map.of(),
                        null
                );
            }
            // Otherwise continue below and classify the new user intent.
        }
        /*
         * Workflow routing.
         * Phase 8 keeps two explicit workflow types only:
         * bill-payment execution and daily account summary.
         */
        if (looksLikeDailySummaryQuestion(value)) {
            return new AgentDecision(
                    "WORKFLOW",
                    "DAILY_ACCOUNT_SUMMARY",
                    1.0,
                    "deterministic daily account summary workflow routing",
                    Map.of(),
                    null
            );
        }

        /*
         * Policy / RAG deterministic routing
         */
        if (looksLikePolicyQuestion(value)) {
            return new AgentDecision(
                    "TOOL",
                    "searchPolicyDocuments",
                    1.0,
                    "deterministic policy/documentation routing",
                    Map.of("query", message == null ? "" : message.trim()),
                    null
            );
        }

        /*
         * Balance routing
         */
        if (looksLikeBalanceQuestion(value)) {
            return new AgentDecision(
                    "TOOL",
                    "getAccountBalance",
                    1.0,
                    "deterministic balance routing",
                    Map.of(),
                    null
            );
        }

        /*
         * Transactions routing
         */
        if (looksLikeTransactionQuestion(value)) {
            return new AgentDecision(
                    "TOOL",
                    "getTransactions",
                    1.0,
                    "deterministic transactions routing",
                    Map.of(),
                    null
            );
        }

        /*
         * Customer profile routing
         */
        if (looksLikeCustomerProfileQuestion(value)) {
            return new AgentDecision(
                    "TOOL",
                    "getCustomerProfile",
                    1.0,
                    "deterministic customer-profile routing",
                    Map.of(),
                    null
            );
        }

        /*
         * Payment status routing
         */
        if (looksLikePaymentStatusQuestion(value)) {
            return new AgentDecision(
                    "TOOL",
                    "getPaymentStatus",
                    1.0,
                    "deterministic payment-status routing",
                    Map.of(),
                    null
            );
        }
        /*
         * Bill payment is a controlled workflow, not a direct tool call.
         * prepareBillPay may be one step inside the workflow, but the route is WORKFLOW.
         */
        if (looksLikeBillPaymentQuestion(value)) {
            return new AgentDecision(
                    "WORKFLOW",
                    "BILL_PAYMENT_EXECUTION",
                    1.0,
                    "deterministic bill-payment execution workflow routing",
                    Map.of(),
                    null
            );
        }

        /*
         * Simple greeting / capability questions
         */
        if (looksLikeGreeting(value)) {
            return new AgentDecision(
                    "DIRECT",
                    null,
                    1.0,
                    "greeting",
                    Map.of(),
                    "Hello! I can help with balances, transactions, bill payments, payment status, and banking policy questions."
            );
        }

        /*
         * Unknown → let the LLM decide.
         */
        return null;
    }

    private boolean looksLikeMissingFieldAnswer(String message, ConversationContext context) {
        String value = message == null ? "" : message.trim();
        String normalized = normalize(message);

        if (value.isBlank() || context == null || context.missingFields() == null || context.missingFields().isEmpty()) {
            return false;
        }

        /*
         * If the user clearly changed intent, do not treat the message as a missing-field answer.
         */
        if (looksLikeNewIntent(normalized)) {
            return false;
        }

        if (context.missingFields().contains("accountId")) {
            return value.toLowerCase().contains("account")
                    || isUuid(value)
                    || value.matches(".*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}.*");
        }

        if (context.missingFields().contains("paymentId")) {
            return value.toLowerCase().contains("payment")
                    || isUuid(value)
                    || value.matches(".*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}.*");
        }

        if (context.missingFields().contains("customerExternalId")) {
            return !looksLikeNewIntent(value.toLowerCase());
        }


        /*
         * Bill-pay missing fields can be natural values, but avoid hijacking obvious new intents.
         */
        return value.length() <= 200;
    }

    private boolean looksLikeNewIntent(String value) {
        return looksLikePolicyQuestion(value)
                || looksLikeBalanceQuestion(value)
                || looksLikeTransactionQuestion(value)
                || looksLikeCustomerProfileQuestion(value)
                || looksLikePaymentStatusQuestion(value)
                || looksLikeDailySummaryQuestion(value)
                || looksLikeBillPaymentQuestion(value)
                || looksLikeGreeting(value);
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value.trim());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private boolean looksLikeGreeting(String value) {
        return containsAny(value,
                "hi",
                "hello",
                "hey",
                "good morning",
                "good evening",
                "what can you do",
                "help");
    }

    private boolean looksLikePolicyQuestion(String value) {
        return containsAny(value,
                "policy",
                "policies",
                "procedure",
                "procedures",
                "documentation",
                "docs",
                "rule",
                "rules",
                "guideline",
                "guidelines",
                "compliance",
                "fdx",
                "kyc",
                "aml",
                "sla",
                "refund",
                "limit",
                "limits",
                "sign up",
                "signup",
                "sign-up",
                "registration",
                "register",
                "onboarding",
                "customer provide",
                "information must a customer provide",
                "required information",
                "required fields",
                "what information",
                "customer application",
                "create customer",
                "open account",
                "account opening",
                "how do i open",
                "how to open");
    }

    private boolean looksLikeBalanceQuestion(String value) {
        return containsAny(value,
                "balance",
                "available balance",
                "current balance",
                "account balance",
                "checking balance",
                "savings balance",
                "how much money",
                "how much do i have",
                "funds available",
                "remaining funds",
                "remaining balance",
                "money left",
                "cash available");
    }

    private boolean looksLikeTransactionQuestion(String value) {
        return containsAny(value,
                "transaction",
                "transactions",
                "transaction history",
                "recent transactions",
                "latest transactions",
                "last transactions",
                "spending history",
                "account activity",
                "statement activity",
                "payments history",
                "debits",
                "credits");
    }

    private boolean looksLikeCustomerProfileQuestion(String value) {
        return containsAny(value,
                "customer info",
                "customer information",
                "customer profile",
                "my customer profile",
                "my profile",
                "profile info",
                "profile information",
                "my details",
                "personal details",
                "profile details",
                "customer details");
    }

    private boolean looksLikePaymentStatusQuestion(String value) {
        return containsAny(value,
                "payment status",
                "status of payment",
                "where is my payment",
                "track payment",
                "payment progress",
                "bill payment status",
                "is my payment posted",
                "payment posted");
    }
    private boolean looksLikeDailySummaryQuestion(String value) {
        return containsAny(value,
                "daily summary",
                "daily account summary",
                "account summary",
                "financial summary",
                "today's summary",
                "today summary",
                "summarize my account",
                "summarise my account",
                "summary of my account");
    }
    private boolean looksLikeBillPaymentQuestion(String value) {
        return containsAny(value,
                "pay bill",
                "bill payment",
                "pay electricity bill",
                "pay hydro bill",
                "prepare payment",
                "make payment",
                "send payment");
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
