package com.bankstack.mcpclient.gateway;

import org.springframework.stereotype.Component;

@Component
public class FallbackToolIntentResolver {

    public String resolve(String message,
                          boolean explicitConfirmation,
                          ConversationContext context) {
        String normalized = message == null ? "" : message.toLowerCase();

        if (explicitConfirmation && context.hasPreparedAction()
                && context.preparedActionType() == PreparedActionType.BILL_PAY) {
            return "confirmBillPay";
        }

        if (containsAny(normalized,
                "policy",
                "policies",
                "procedure",
                "procedures",
                "compliance",
                "rule",
                "rules",
                "fdx",
                "open banking standard",
                "what does the policy say",
                "knowledge base",
                "documentation",
                "docs",
                "explain kyc",
                "kyc policy",
                "aml policy",
                "how does this process work",
                "sla",
                "guideline",
                "architecture",
                "microservices",
                "spring boot",
                "istio",
                "service mesh",
                "code review",
                "design",
                "sign up",
                "signup",
                "sign-up",
                "registration",
                "register",
                "customer registration",
                "customer onboarding",
                "onboarding",
                "customer provide",
                "information must a customer provide",
                "required information",
                "required fields",
                "customer application",
                "create customer",
                "open account",
                "account opening")) {
            return "searchPolicyDocuments";
        }

        if (containsAny(normalized, "balance")) {
            return "getAccountBalance";
        }

        if (containsAny(normalized, "transaction", "transactions")) {
            return "getTransactions";
        }

        if (containsAny(normalized, "customer profile", "profile")) {
            return "getCustomerProfile";
        }

        if (containsAny(normalized, "payment status", "status of payment", "status")) {
            return "getPaymentStatus";
        }

        if (containsAny(normalized, "bill pay", "pay my", "pay bill", "biller")) {
            return "prepareBillPay";
        }

        return null;
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
