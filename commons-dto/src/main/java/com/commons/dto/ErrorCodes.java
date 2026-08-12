package com.commons.dto;

public final class ErrorCodes {

    private ErrorCodes() {}

    public static final String INVALID_JWT = "INVALID_JWT";
    public static final String INVALID_CREDS = "INVALID_CREDS";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String INSUFFICIENT_SCOPE = "INSUFFICIENT_SCOPE";

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INVALID_INPUT = "INVALID_INPUT";

    public static final String CONFLICT = "CONFLICT";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    public static final String CUSTOMER_NOT_FOUND = "CUSTOMER_NOT_FOUND";
    public static final String CONSENT_MISSING = "CONSENT_MISSING";

    public static final String OWNERSHIP_VIOLATION = "OWNERSHIP_VIOLATION";
    public static final String CONFIRMATION_TOKEN_INVALID = "CONFIRMATION_TOKEN_INVALID";
    public static final String CONFIRMATION_TOKEN_EXPIRED = "CONFIRMATION_TOKEN_EXPIRED";
    public static final String CONFIRMATION_TOOL_MISMATCH = "CONFIRMATION_TOOL_MISMATCH";
    public static final String CONFIRMATION_OWNERSHIP_MISMATCH = "CONFIRMATION_OWNERSHIP_MISMATCH";
    public static final String MISSING_CUSTOMER_CONTEXT = "MISSING_CUSTOMER_CONTEXT";

    public static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
    public static final String INVALID_TRANSITION = "INVALID_TRANSITION";
    public static final String VERSION_MISMATCH = "VERSION_MISMATCH";
    public static final String PRECONDITION_REQUIRED = "PRECONDITION_REQUIRED";

    public static final String DOWNSTREAM_UNAUTHORIZED = "DOWNSTREAM_UNAUTHORIZED";
    public static final String DOWNSTREAM_FORBIDDEN = "DOWNSTREAM_FORBIDDEN";
    public static final String DOWNSTREAM_NOT_FOUND = "DOWNSTREAM_NOT_FOUND";
    public static final String DOWNSTREAM_UNAVAILABLE = "DOWNSTREAM_UNAVAILABLE";
    public static final String DOWNSTREAM_ERROR = "DOWNSTREAM_ERROR";

    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
}
