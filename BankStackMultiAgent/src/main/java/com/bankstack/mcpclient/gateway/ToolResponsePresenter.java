package com.bankstack.mcpclient.gateway;

import com.commons.dto.ToolExecutionResult;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ToolResponsePresenter {

    public String present(String toolName, ToolExecutionResult result) {
        if (result == null) {
            return "I’m unable to complete this request right now.";
        }

        if (result.failed() || result.needsInput() || result.prepared() || result.executed()) {
            return safeMessage(result, "I’m unable to complete this request right now.");
        }

        Map<String, Object> data = result.getData();
        Object payload = data == null ? null : data.get("result");

        if ("getAccountBalance".equals(toolName) && payload instanceof Map<?, ?> balance) {
            Object availableBalance = firstNonNull(balance, "availableBalance", "available", "balance", "currentBalance");
            Object currency = firstNonNull(balance, "currency", "currencyCode");

            if (availableBalance != null && currency != null) {
                return "Your current available balance is " + availableBalance + " " + currency + ".";
            }
        }

        if ("getPaymentStatus".equals(toolName) && payload instanceof Map<?, ?> payment) {
            Object state = firstNonNull(payment, "state", "status", "paymentStatus");
            Object paymentId = firstNonNull(payment, "paymentId", "id");

            if (state != null) {
                return paymentId == null
                        ? "Your payment status is " + state + "."
                        : "Payment " + paymentId + " is currently " + state + ".";
            }
        }

        if ("getTransactions".equals(toolName) && payload instanceof Map<?, ?> transactions) {
            Object count = firstNonNull(transactions, "count", "total", "size");
            if (count != null) {
                return "I retrieved " + count + " transaction(s).";
            }
        }
        if ("searchPolicyDocuments".equals(toolName)) {
            String answer = extractPolicyAnswer(payload);
            if (answer != null && !answer.isBlank()) {
                return answer;
            }
        }

        return safeMessage(result, "The request completed successfully.");
    }

    private String extractPolicyAnswer(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            Object answer = firstNonNull(map, "answer", "message", "summary", "response");
            return answer == null ? null : answer.toString();
        }
        return payload == null ? null : payload.toString();
    }

    private Object firstNonNull(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String safeMessage(ToolExecutionResult result, String fallback) {
        return result.getMessage() == null || result.getMessage().isBlank()
                ? fallback
                : result.getMessage();
    }
}
