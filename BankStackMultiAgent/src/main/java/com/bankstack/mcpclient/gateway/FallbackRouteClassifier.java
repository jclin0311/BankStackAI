package com.bankstack.mcpclient.gateway;

import org.springframework.stereotype.Component;

@Component
public class FallbackRouteClassifier {

    public Route classify(String message) {
        String normalized = message == null ? "" : message.toLowerCase();

        if (containsAny(normalized,
                "daily summary",
                "account summary",
                "financial summary",
                "pay bill",
                "bill payment",
                "pay hydro bill",
                "pay electricity bill",
                "send payment")) {
            return Route.WORKFLOW;
        }

        if (containsAny(normalized,
                "balance",
                "transaction",
                "transactions",
                "account",
                "customer profile",
                "payment status",
                "bill pay",
                "pay my",
                "transfer money",
                "send money",
                "debit",
                "credit",
                "dispute",
                "beneficiary",
                "biller",
                "confirm payment",
                "confirm it",
                "policy",
                "policies",
                "procedure",
                "procedures",
                "compliance",
                "rule",
                "rules",
                "fdx",
                "open banking standard",
                "knowledge base",
                "documentation",
                "docs",
                "kyc",
                "aml",
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
                "required information",
                "required fields",
                "customer application",
                "create customer",
                "open account",
                "account opening",
                "account id",
                "account number")) {
            return Route.TOOL;
        }

        return Route.DIRECT;
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