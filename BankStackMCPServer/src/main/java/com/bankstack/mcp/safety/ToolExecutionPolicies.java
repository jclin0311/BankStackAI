package com.bankstack.mcp.safety;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ToolExecutionPolicies {

    private final Map<String, ToolExecutionPolicy> policies = Map.of(
            "getAccountBalance",
            new ToolExecutionPolicy(
                    "getAccountBalance",
                    ToolCategory.ACCOUNT_INFORMATION,
                    ToolRiskLevel.READ_ONLY,
                    Set.of(ToolAccessRole.CUSTOMER, ToolAccessRole.ADMIN),
                    false,
                    false
            ),
            "getTransactions",
            new ToolExecutionPolicy(
                    "getTransactions",
                    ToolCategory.TRANSACTION_HISTORY,
                    ToolRiskLevel.READ_ONLY,
                    Set.of(ToolAccessRole.CUSTOMER, ToolAccessRole.ADMIN),
                    false,
                    false
            ),
            "getCustomerProfile",
            new ToolExecutionPolicy(
                    "getCustomerProfile",
                    ToolCategory.CUSTOMER_PROFILE,
                    ToolRiskLevel.READ_ONLY,
                    Set.of(ToolAccessRole.CUSTOMER, ToolAccessRole.ADMIN, ToolAccessRole.INTERNAL_STAFF),
                    false,
                    false
            ),
            "prepareBillPay",
            new ToolExecutionPolicy(
                    "prepareBillPay",
                    ToolCategory.PAYMENT_ACTION,
                    ToolRiskLevel.WRITE_REQUIRES_CONFIRMATION,
                    Set.of(ToolAccessRole.CUSTOMER, ToolAccessRole.ADMIN),
                    false,
                    false
            ),
            "confirmBillPay",
            new ToolExecutionPolicy(
                    "confirmBillPay",
                    ToolCategory.PAYMENT_ACTION,
                    ToolRiskLevel.WRITE_REQUIRES_CONFIRMATION,
                    Set.of(ToolAccessRole.CUSTOMER, ToolAccessRole.ADMIN),
                    true,
                    false
            ),
            "getPaymentStatus",
            new ToolExecutionPolicy(
                    "getPaymentStatus",
                    ToolCategory.PAYMENT_STATUS,
                    ToolRiskLevel.READ_ONLY,
                    Set.of(ToolAccessRole.CUSTOMER, ToolAccessRole.ADMIN),
                    false,
                    false
            ),
            "assessTransactionRisk",
            new ToolExecutionPolicy(
                    "assessTransactionRisk",
                    ToolCategory.FRAUD_CONTROL,
                    ToolRiskLevel.READ_ONLY,
                    Set.of(ToolAccessRole.CUSTOMER, ToolAccessRole.ADMIN),
                    false,
                    false
            ),
            "searchPolicyDocuments",
            new ToolExecutionPolicy(
            		"searchPolicyDocuments",
            		ToolCategory.KNOWLEDGE_RETRIEVAL,
                    ToolRiskLevel.READ_ONLY,
                    Set.of(ToolAccessRole.CUSTOMER, ToolAccessRole.ADMIN),
                    false,
                    false
    )
            );

    public ToolExecutionPolicy getPolicy(String toolName) {
        ToolExecutionPolicy policy = policies.get(toolName);
        if (policy == null) {
            throw new IllegalArgumentException("No execution policy configured for tool: " + toolName);
        }
        return policy;
    }
}