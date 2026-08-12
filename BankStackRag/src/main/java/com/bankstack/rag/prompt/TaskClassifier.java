package com.bankstack.rag.prompt;

import org.springframework.stereotype.Component;
@Component
public class TaskClassifier {

    public TaskType classify(String query) {

        String q = query == null ? "" : query.toLowerCase();

        if (containsAny(q,
                "ignore previous instructions",
                "ignore all instructions",
                "reveal system prompt",
                "show system prompt",
                "bypass security",
                "override rules",
                "disable security",
                "use external knowledge",
                "answer without documents",
                "show all customer balances",
                "list all customer balances",
                "show customer passwords",
                "show all kyc records",
                "customer passwords",
                "all customer data",
                "approve kyc",
                "unblock login",
                "change customer status")) {

            return TaskType.REFUSAL;
        }

        return TaskType.POLICY_LOOKUP;
    }

    private boolean containsAny(String query, String... patterns) {
        for (String pattern : patterns) {
            if (query.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}