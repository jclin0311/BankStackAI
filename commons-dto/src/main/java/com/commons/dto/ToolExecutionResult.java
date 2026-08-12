package com.commons.dto;

import java.util.List;
import java.util.Map;

public class ToolExecutionResult {

    private String status; // SUCCESS | FAILED | NEEDS_INPUT | PREPARED | EXECUTED
    private String message;
    private String actionType;
    private boolean requiresConfirmation;
    private String confirmationToken;
    private List<String> missingFields;
    private Map<String, Object> data;

    public ToolExecutionResult() {
    }

    public ToolExecutionResult(String status,
                               String message,
                               String actionType,
                               boolean requiresConfirmation,
                               String confirmationToken,
                               List<String> missingFields,
                               Map<String, Object> data) {
        this.status = status;
        this.message = message == null ? "" : message;
        this.actionType = actionType;
        this.requiresConfirmation = requiresConfirmation;
        this.confirmationToken = confirmationToken;
        this.missingFields = missingFields == null ? List.of() : missingFields;
        this.data = data == null ? Map.of() : data;
    }

    public static ToolExecutionResult success(String message,
                                              String actionType,
                                              Map<String, Object> data) {
        return new ToolExecutionResult(
                "SUCCESS",
                message,
                actionType,
                false,
                null,
                List.of(),
                data
        );
    }

    public static ToolExecutionResult needInput(String message,
                                                List<String> missingFields,
                                                String actionType) {
        return new ToolExecutionResult(
                "NEEDS_INPUT",
                message,
                actionType,
                false,
                null,
                missingFields,
                Map.of()
        );
    }

    public static ToolExecutionResult prepared(String message,
                                               String confirmationToken,
                                               String actionType,
                                               Map<String, Object> data) {
        return new ToolExecutionResult(
                "PREPARED",
                message,
                actionType,
                true,
                confirmationToken,
                List.of(),
                data
        );
    }

    public static ToolExecutionResult executed(String message,
                                               String actionType,
                                               Map<String, Object> data) {
        return new ToolExecutionResult(
                "EXECUTED",
                message,
                actionType,
                false,
                null,
                List.of(),
                data
        );
    }

    public static ToolExecutionResult failed(String message,
                                             String actionType,
                                             Map<String, Object> data) {
        return new ToolExecutionResult(
                "FAILED",
                message,
                actionType,
                false,
                null,
                List.of(),
                data
        );
    }

    public static ToolExecutionResult failed(String message,
                                             String actionType,
                                             String errorCode,
                                             boolean retryable) {
        return new ToolExecutionResult(
                "FAILED",
                message,
                actionType,
                false,
                null,
                List.of(),
                Map.of(
                        "errorCode", errorCode,
                        "retryable", retryable
                )
        );
    }
    

    public boolean succeeded() {
        return "SUCCESS".equalsIgnoreCase(status);
    }

    public boolean failed() {
        return "FAILED".equalsIgnoreCase(status);
    }

    public boolean needsInput() {
        return "NEEDS_INPUT".equalsIgnoreCase(status);
    }

    public boolean prepared() {
        return "PREPARED".equalsIgnoreCase(status);
    }

    public boolean executed() {
        return "EXECUTED".equalsIgnoreCase(status);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? "" : message;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public boolean isRequiresConfirmation() {
        return requiresConfirmation;
    }

    public void setRequiresConfirmation(boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    public List<String> getMissingFields() {
        return missingFields == null ? List.of() : missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields == null ? List.of() : missingFields;
    }

    public Map<String, Object> getData() {
        return data == null ? Map.of() : data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data == null ? Map.of() : data;
    }
}