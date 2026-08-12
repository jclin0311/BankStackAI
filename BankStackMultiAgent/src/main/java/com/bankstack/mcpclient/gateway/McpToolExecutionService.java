package com.bankstack.mcpclient.gateway;

import com.commons.dto.ToolExecutionResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class McpToolExecutionService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SyncMcpToolCallbackProvider toolCallbackProvider;

    public McpToolExecutionService(SyncMcpToolCallbackProvider toolCallbackProvider) {
        this.toolCallbackProvider = toolCallbackProvider;
    }

    public ToolExecutionResult execute(String intendedTool, Map<String, Object> toolArguments) {
        try {
            ToolCallback toolCallback = callbackByName().get(intendedTool);
            if (toolCallback == null) {
                throw new IllegalStateException("Approved MCP tool is not currently registered: " + intendedTool);
            }

            String inputJson = MAPPER.writeValueAsString(toolArguments == null ? Map.of() : toolArguments);
            String rawToolResult = toolCallback.call(inputJson);

            return normalizeToolResult(intendedTool, rawToolResult);

        } catch (Exception ex) {
            return ToolExecutionResult.failed(
                    failureMessageFor(intendedTool),
                    actionTypeFor(intendedTool),
                    Map.of("error", safeError(ex))
            );
        }
    }

    private ToolExecutionResult normalizeToolResult(String intendedTool, String rawToolResult) {
        if (rawToolResult == null || rawToolResult.isBlank()) {
            return ToolExecutionResult.failed(
                    "Invalid MCP response. Empty response received from tool.",
                    actionTypeFor(intendedTool),
                    Map.of("contractError", "EMPTY_RESPONSE")
            );
        }

        try {
            String contractJson = unwrapMcpTextContent(rawToolResult);
            ToolExecutionResult result = MAPPER.readValue(contractJson, ToolExecutionResult.class);
            validateContract(intendedTool, result);
            return result;

        } catch (Exception ex) {
            return ToolExecutionResult.failed(
                    "Invalid MCP response contract from " + intendedTool + ".",
                    actionTypeFor(intendedTool),
                    Map.of(
                            "contractError", "INVALID_TOOL_EXECUTION_RESULT",
                            "raw", rawToolResult,
                            "error", safeError(ex)
                    )
            );
        }
    }

    private String unwrapMcpTextContent(String rawToolResult) throws Exception {
        String trimmed = rawToolResult.trim();

        if (trimmed.startsWith("{")) {
            return trimmed;
        }

        if (trimmed.startsWith("[")) {
            List<Map<String, Object>> content = MAPPER.readValue(trimmed, new TypeReference<>() {});

            if (content.isEmpty()) {
                throw new IllegalArgumentException("MCP content array is empty");
            }

            Object text = content.get(0).get("text");
            if (text == null || text.toString().isBlank()) {
                throw new IllegalArgumentException("MCP content text is missing");
            }

            return text.toString();
        }

        throw new IllegalArgumentException("Unsupported MCP tool response format");
    }

    private void validateContract(String intendedTool, ToolExecutionResult result) {
        if (result == null) {
            throw new IllegalArgumentException("ToolExecutionResult is null");
        }

        if (result.getStatus() == null || result.getStatus().isBlank()) {
            throw new IllegalArgumentException("Missing status");
        }

        String status = result.getStatus().trim().toUpperCase();
        if (!List.of("SUCCESS", "FAILED", "NEEDS_INPUT", "NEED_INPUT", "PREPARED", "EXECUTED").contains(status)) {
            throw new IllegalArgumentException("Unsupported status: " + result.getStatus());
        }

        if (result.getActionType() == null || result.getActionType().isBlank()) {
            result.setActionType(actionTypeFor(intendedTool));
        }

        if (("NEEDS_INPUT".equals(status) || "NEED_INPUT".equals(status))
                && (result.getMissingFields() == null || result.getMissingFields().isEmpty())) {
            throw new IllegalArgumentException("NEEDS_INPUT requires missingFields");
        }

        if ("PREPARED".equals(status)
                && (result.getConfirmationToken() == null || result.getConfirmationToken().isBlank())) {
            throw new IllegalArgumentException("PREPARED requires confirmationToken");
        }
    }

    private Map<String, ToolCallback> callbackByName() {
        return Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .collect(Collectors.toMap(
                        callback -> callback.getToolDefinition().name(),
                        Function.identity(),
                        (left, right) -> left
                ));
    }

    private String actionTypeFor(String toolName) {
        return switch (toolName) {
            case "searchPolicyDocuments" -> "SEARCH_POLICY_DOCUMENTS";
            case "getAccountBalance" -> "READ_ACCOUNT_BALANCE";
            case "getTransactions" -> "READ_TRANSACTIONS";
            case "getCustomerProfile" -> "READ_CUSTOMER_PROFILE";
            case "getPaymentStatus" -> "PAYMENT_STATUS";
            case "prepareBillPay", "confirmBillPay" -> "BILL_PAY";
            default -> toolName;
        };
    }

    private String failureMessageFor(String toolName) {
        return switch (toolName) {
            case "searchPolicyDocuments" ->
                    "I’m unable to search policy documents right now. Please try again later.";
            case "getAccountBalance" ->
                    "I’m unable to retrieve your account balance right now. Please try again later.";
            case "getTransactions" ->
                    "I’m unable to retrieve your transactions right now. Please try again later.";
            case "getCustomerProfile" ->
                    "I’m unable to retrieve your customer profile right now. Please try again later.";
            case "getPaymentStatus" ->
                    "I’m unable to retrieve the payment status right now. Please try again later.";
            case "assessTransactionRisk" ->
                    "I’m unable to assess transaction risk right now. Please try again later.";
            case "prepareBillPay" ->
                    "I’m unable to prepare your bill payment right now. Please try again later.";
            case "confirmBillPay" ->
                    "I’m unable to confirm your bill payment right now. No payment has been executed.";
            default ->
                    "I’m unable to complete this request right now. Please try again later.";
        };
    }

    private String safeError(Exception ex) {
        if (ex == null) {
            return "Unknown error";
        }
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
