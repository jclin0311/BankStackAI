package com.bankstack.mcpclient.workflow;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class WorkflowPlanFactory {

    public WorkflowPlan create(String message) {
        WorkflowType type = detect(message);
        return create(type);
    }

    public WorkflowPlan create(WorkflowType type) {
        return switch (type) {
            case BILL_PAYMENT_EXECUTION -> billPaymentExecution();
            case DAILY_ACCOUNT_SUMMARY -> dailyAccountSummary();
        };
    }

    public WorkflowType detect(String message) {
        String value = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (containsAny(value, "daily summary", "daily account summary",
                "account summary", "financial summary", "today summary")) {
            return WorkflowType.DAILY_ACCOUNT_SUMMARY;
        }

        return WorkflowType.BILL_PAYMENT_EXECUTION;
    }

    private WorkflowPlan billPaymentExecution() {
        return new WorkflowPlan(
                WorkflowType.BILL_PAYMENT_EXECUTION,
                "Bill payment execution workflow",
                "Validate bill payment intent, prepare the payment, require explicit confirmation, execute, audit, and respond.",
                true,
                List.of(
                        WorkflowStep.standard(WorkflowStepType.VALIDATE, "Validate payment intent"),
                        WorkflowStep.standard(WorkflowStepType.PLAN, "Create controlled payment plan"),
                        WorkflowStep.tool("Prepare bill payment", "prepareBillPay", true, false),
                        WorkflowStep.standard(WorkflowStepType.CONFIRM, "Require explicit user confirmation"),
                        WorkflowStep.tool("Execute confirmed bill payment", "confirmBillPay", false, true),
                        WorkflowStep.standard(WorkflowStepType.AUDIT, "Audit workflow outcome"),
                        WorkflowStep.standard(WorkflowStepType.RESPOND, "Respond to customer")
                )
        );
    }

    private WorkflowPlan dailyAccountSummary() {
        return new WorkflowPlan(
                WorkflowType.DAILY_ACCOUNT_SUMMARY,
                "Daily account summary workflow",
                "Collect balance and recent transactions, combine the evidence, audit the workflow, and produce a clear daily summary.",
                false,
                List.of(
                        WorkflowStep.standard(WorkflowStepType.VALIDATE, "Validate account summary request"),
                        WorkflowStep.standard(WorkflowStepType.PLAN, "Plan account summary"),
                        WorkflowStep.tool("Get account balance", "getAccountBalance", true, false),
                        WorkflowStep.tool("Get recent transactions", "getTransactions", true, false),
                        WorkflowStep.standard(WorkflowStepType.AUDIT, "Audit summary workflow"),
                        WorkflowStep.standard(WorkflowStepType.RESPOND, "Respond with daily summary")
                )
        );
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
