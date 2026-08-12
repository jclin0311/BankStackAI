package com.bankstack.mcp.service;

import com.bankstack.mcp.api.ToolInvokeRequest;
import com.commons.exception.InvalidToolInputException;
import com.bankstack.mcp.tools.BankStackTools;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class ToolInvocationService {

    private final BankStackTools bankStackTools;

    public ToolInvocationService(BankStackTools bankStackTools) {
        this.bankStackTools = bankStackTools;
    }

    public Map<String, Object> listTools() {
        return Map.of(
                "message", "Manual MCP tool testing endpoint. In Phase 4, the agent runtime will choose and invoke these tools.",
                "tools", List.of(
                        Map.of(
                                "name", "getAccountBalance",
                                "description", "Get the balance for a specific account.",
                                "input", List.of("accountId")
                        ),
                        Map.of(
                                "name", "getTransactions",
                                "description", "Get transactions for an account with optional filters.",
                                "input", List.of("accountId", "startDate", "endDate", "type", "limit", "offset")
                        ),
                        Map.of(
                                "name", "getCustomerProfile",
                                "description", "Get customer profile using externalId.",
                                "input", List.of("customerExternalId")
                        ),
                        Map.of(
                                "name", "assessTransactionRisk",
                                "description", "Read-only fraud control tool that assesses risk before payment execution.",
                                "input", List.of("accountId", "amount", "payee", "channel")
                        ),
                        Map.of(
                                "name", "prepareBillPay",
                                "description", "Prepare a bill payment and return a confirmation token. Internally checks transaction risk first.",
                                "input", List.of(
                                        "debtorAccountId",
                                        "billerReferenceNumber",
                                        "invoiceReference",
                                        "executionDate",
                                        "amount",
                                        "currency",
                                        "note",
                                        "idempotencyKey"
                                )
                        ),
                        Map.of(
                                "name", "searchPolicyDocuments",
                                "description", "Search internal bank policy documents through the RAG system.",
                                "input", List.of("query")
                        ),
                        Map.of(
                                "name", "confirmBillPay",
                                "description", "Confirm and execute a previously prepared bill payment.",
                                "input", List.of("confirmationToken")
                        ),
                        Map.of(
                                "name", "getPaymentStatus",
                                "description", "Get the latest payment status.",
                                "input", List.of("paymentId")
                        )
                )
        );
    }

    public Object invoke(ToolInvokeRequest request) {
        if (request == null) {
            throw new InvalidToolInputException("Request body is required.");
        }

        String tool = requiredString(request.tool(), "tool");
        Map<String, Object> input = request.input() == null ? Map.of() : request.input();

        return switch (tool) {
            case "getAccountBalance" -> bankStackTools.getAccountBalance(
                    stringValue(input, "accountId")
            );

            case "getTransactions" -> bankStackTools.getTransactions(
                    stringValue(input, "accountId"),
                    optionalStringValue(input, "startDate"),
                    optionalStringValue(input, "endDate"),
                    optionalStringValue(input, "type"),
                    integerValue(input, "limit"),
                    integerValue(input, "offset")
            );

            case "getCustomerProfile" -> bankStackTools.getCustomerProfile(
                    stringValue(input, "customerExternalId")
            );
            case "searchPolicyDocuments" -> bankStackTools.searchPolicyDocuments(
                    stringValue(input, "query")
            );


            case "prepareBillPay" -> bankStackTools.prepareBillPay(
                    stringValue(input, "debtorAccountId"),
                    stringValue(input, "billerReferenceNumber"),
                    stringValue(input, "invoiceReference"),
                    stringValue(input, "executionDate"),
                    bigDecimalValue(input, "amount"),
                    stringValue(input, "currency"),
                    optionalStringValue(input, "note"),
                    optionalStringValue(input, "idempotencyKey")
            );

            case "confirmBillPay" -> bankStackTools.confirmBillPay(
                    stringValue(input, "confirmationToken")
            );

            case "getPaymentStatus" -> bankStackTools.getPaymentStatus(
                    stringValue(input, "paymentId")
            );

            default -> throw new InvalidToolInputException("Unsupported tool: " + tool);
        };
    }

    private String requiredString(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidToolInputException(fieldName + " is required.");
        }
        return value.trim();
    }

    private String stringValue(Map<String, Object> input, String fieldName) {
        Object value = input.get(fieldName);
        if (value == null) {
            throw new InvalidToolInputException(fieldName + " is required.");
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            throw new InvalidToolInputException(fieldName + " is required.");
        }
        return text;
    }

    private String optionalStringValue(Map<String, Object> input, String fieldName) {
        Object value = input.get(fieldName);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private Integer integerValue(Map<String, Object> input, String fieldName) {
        Object value = input.get(fieldName);
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof Number number) {
                return number.intValue();
            }
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ex) {
            throw new InvalidToolInputException(fieldName + " must be a valid integer.");
        }
    }

    private BigDecimal bigDecimalValue(Map<String, Object> input, String fieldName) {
        Object value = input.get(fieldName);
        if (value == null) {
            throw new InvalidToolInputException(fieldName + " is required.");
        }

        try {
            if (value instanceof Number number) {
                return new BigDecimal(number.toString());
            }
            return new BigDecimal(String.valueOf(value).trim());
        } catch (Exception ex) {
            throw new InvalidToolInputException(fieldName + " must be a valid decimal number.");
        }
    }
}