package com.bankstack.mcpclient.gateway;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ApprovedToolAccessGuard {

    private static final Set<String> APPROVED_TOOLS = Set.of(
            "getAccountBalance",
            "getTransactions",
            "getCustomerProfile",
            "searchPolicyDocuments",
            "prepareBillPay",
            "confirmBillPay",
            "getPaymentStatus",
            "assessTransactionRisk"
    );

    public void validate(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalStateException("Could not determine an approved MCP tool for this request.");
        }

        if (!APPROVED_TOOLS.contains(toolName)) {
            throw new IllegalStateException("Requested tool is not approved for the generic banking agent.");
        }
    }
}
