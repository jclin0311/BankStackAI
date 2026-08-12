package com.bankstack.mcpclient.gateway;

import com.commons.dto.ToolExecutionResult;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class ToolArgumentContractValidator {

    public ToolExecutionResult validate(String toolName, Map<String, Object> input) {
        Map<String, Object> args = input == null ? Map.of() : input;

        return switch (toolName) {
            case "searchPolicyDocuments" -> requireFields("SEARCH_POLICY_DOCUMENTS", args, "query");
            case "getAccountBalance" -> validateUuid("READ_ACCOUNT_BALANCE", args, "accountId");
            case "getTransactions" -> validateTransactions(args);
            case "getCustomerProfile" -> requireFields("READ_CUSTOMER_PROFILE", args, "customerExternalId");
            case "getPaymentStatus" -> validateUuid("PAYMENT_STATUS", args, "paymentId");
            case "prepareBillPay" -> validatePrepareBillPay(args);
            case "confirmBillPay" -> requireFields("BILL_PAY", args, "confirmationToken");
            default -> ToolExecutionResult.failed(
                    "Unsupported MCP tool selected.",
                    toolName,
                    Map.of("toolName", toolName == null ? "" : toolName)
            );
        };
    }

    private ToolExecutionResult validateTransactions(Map<String, Object> args) {
        ToolExecutionResult accountValidation = validateUuid("READ_TRANSACTIONS", args, "accountId");
        if (accountValidation.needsInput() || accountValidation.failed()) {
            return accountValidation;
        }

        ToolExecutionResult startDateValidation = validateOptionalOffsetDateTime(args, "startDate", "READ_TRANSACTIONS");
        if (startDateValidation.needsInput()) {
            return startDateValidation;
        }

        ToolExecutionResult endDateValidation = validateOptionalOffsetDateTime(args, "endDate", "READ_TRANSACTIONS");
        if (endDateValidation.needsInput()) {
            return endDateValidation;
        }

        if (!isBlank(args.get("startDate")) && !isBlank(args.get("endDate"))) {
            OffsetDateTime start = OffsetDateTime.parse(args.get("startDate").toString());
            OffsetDateTime end = OffsetDateTime.parse(args.get("endDate").toString());
            if (end.isBefore(start)) {
                return ToolExecutionResult.needInput(
                        "Please provide an endDate that is after startDate.",
                        List.of("endDate"),
                        "READ_TRANSACTIONS"
                );
            }
        }

        ToolExecutionResult limitValidation = validateOptionalIntegerRange(args, "limit", 1, 100, "READ_TRANSACTIONS");
        if (limitValidation.needsInput()) {
            return limitValidation;
        }

        ToolExecutionResult offsetValidation = validateOptionalIntegerRange(args, "offset", 0, 100_000, "READ_TRANSACTIONS");
        if (offsetValidation.needsInput()) {
            return offsetValidation;
        }

        return ToolExecutionResult.success("Tool argument validation passed.", "READ_TRANSACTIONS", Map.of());
    }


   

    private ToolExecutionResult validateRiskAssessment(Map<String, Object> args) {
        List<String> missing = missingFields(args, "accountId", "amount", "payee", "channel");

        if (!isUuid(args.get("accountId"))) {
            return ToolExecutionResult.needInput(
                    "Please provide a valid accountId.",
                    List.of("accountId"),
                    "FRAUD_RISK_ASSESSMENT"
            );
        }

        try {
            BigDecimal amount = new BigDecimal(args.get("amount").toString());
            if (amount.signum() <= 0) {
                return ToolExecutionResult.needInput(
                        "Please provide a positive amount.",
                        List.of("amount"),
                        "FRAUD_RISK_ASSESSMENT"
                );
            }
        } catch (Exception ex) {
            return ToolExecutionResult.needInput(
                    "Please provide a valid amount.",
                    List.of("amount"),
                    "FRAUD_RISK_ASSESSMENT"
            );
        }

        return ToolExecutionResult.success("Tool argument validation passed.", "FRAUD_RISK_ASSESSMENT", Map.of());
    }

    private ToolExecutionResult validatePrepareBillPay(Map<String, Object> args) {
        List<String> missing = missingFields(args,
                "debtorAccountId",
                "billerReferenceNumber",
                "invoiceReference",
                "executionDate",
                "amount",
                "currency"
        );

        if (!missing.isEmpty()) {
            return ToolExecutionResult.needInput(
                    "Please provide the missing bill payment details: " + String.join(", ", missing) + ".",
                    missing,
                    "BILL_PAY"
            );
        }

        if (!isUuid(args.get("debtorAccountId"))) {
            return ToolExecutionResult.needInput(
                    "Please provide a valid debtorAccountId.",
                    List.of("debtorAccountId"),
                    "BILL_PAY"
            );
        }

        try {
            BigDecimal amount = new BigDecimal(args.get("amount").toString());
            if (amount.signum() <= 0) {
                return ToolExecutionResult.needInput("Please provide a positive amount.", List.of("amount"), "BILL_PAY");
            }
        } catch (Exception ex) {
            return ToolExecutionResult.needInput("Please provide a valid amount.", List.of("amount"), "BILL_PAY");
        }

        String currency = args.get("currency").toString().trim().toUpperCase();
        if (!Set.of("CAD", "USD").contains(currency)) {
            return ToolExecutionResult.needInput(
                    "Please provide a supported currency: CAD or USD.",
                    List.of("currency"),
                    "BILL_PAY"
            );
        }

        try {
            LocalDate.parse(args.get("executionDate").toString().trim());
        } catch (DateTimeParseException ex) {
            return ToolExecutionResult.needInput(
                    "Please provide executionDate in yyyy-MM-dd format.",
                    List.of("executionDate"),
                    "BILL_PAY"
            );
        }

        return ToolExecutionResult.success("Tool argument validation passed.", "BILL_PAY", Map.of());
    }

    private ToolExecutionResult validateUuid(String actionType, Map<String, Object> args, String field) {
        ToolExecutionResult required = requireFields(actionType, args, field);
        if (required.needsInput()) {
            return required;
        }

        if (!isUuid(args.get(field))) {
            return ToolExecutionResult.needInput(
                    "Please provide a valid " + field + ".",
                    List.of(field),
                    actionType
            );
        }

        return ToolExecutionResult.success("Tool argument validation passed.", actionType, Map.of());
    }

    private ToolExecutionResult requireFields(String actionType, Map<String, Object> args, String... fields) {
        List<String> missing = missingFields(args, fields);

        if (!missing.isEmpty()) {
            return ToolExecutionResult.needInput(
                    "Please provide: " + String.join(", ", missing) + ".",
                    missing,
                    actionType
            );
        }

        return ToolExecutionResult.success("Tool argument validation passed.", actionType, Map.of());
    }

    private ToolExecutionResult validateOptionalOffsetDateTime(Map<String, Object> args,
                                                              String field,
                                                              String actionType) {
        Object value = args.get(field);
        if (isBlank(value)) {
            return ToolExecutionResult.success("No optional date validation required.", actionType, Map.of());
        }

        try {
            OffsetDateTime.parse(value.toString().trim());
            return ToolExecutionResult.success("Date validation passed.", actionType, Map.of());
        } catch (DateTimeParseException ex) {
            return ToolExecutionResult.needInput(
                    "Please provide " + field + " as a valid ISO-8601 date-time, for example 2016-05-04T00:00:00Z.",
                    List.of(field),
                    actionType
            );
        }
    }

    private ToolExecutionResult validateOptionalIntegerRange(Map<String, Object> args,
                                                             String field,
                                                             int min,
                                                             int max,
                                                             String actionType) {
        Object value = args.get(field);
        if (isBlank(value)) {
            return ToolExecutionResult.success("No optional integer validation required.", actionType, Map.of());
        }

        try {
            int parsed = Integer.parseInt(value.toString().trim());
            if (parsed < min || parsed > max) {
                return ToolExecutionResult.needInput(
                        "Please provide " + field + " between " + min + " and " + max + ".",
                        List.of(field),
                        actionType
                );
            }
            return ToolExecutionResult.success("Integer validation passed.", actionType, Map.of());
        } catch (NumberFormatException ex) {
            return ToolExecutionResult.needInput(
                    "Please provide a valid numeric " + field + ".",
                    List.of(field),
                    actionType
            );
        }
    }

    private List<String> missingFields(Map<String, Object> args, String... fields) {
        List<String> missing = new ArrayList<>();
        for (String field : fields) {
            if (isBlank(args.get(field))) {
                missing.add(field);
            }
        }
        return missing;
    }

    private boolean isUuid(Object value) {
        if (isBlank(value)) {
            return false;
        }

        try {
            UUID.fromString(value.toString().trim());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isBlank(Object value) {
        return value == null || value.toString().trim().isEmpty();
    }
}
