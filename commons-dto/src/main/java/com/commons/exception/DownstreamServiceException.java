package com.commons.exception;

public class DownstreamServiceException extends McpToolException {

    private final String serviceName;
    private final String operation;
    private final int statusCode;
    private final String safeMessage;

    public DownstreamServiceException(String serviceName, String operation, int statusCode, String safeMessage) {
        super(safeMessage);
        this.serviceName = serviceName;
        this.operation = operation;
        this.statusCode = statusCode;
        this.safeMessage = safeMessage;
    }

    public DownstreamServiceException(String serviceName,
                                      String operation,
                                      int statusCode,
                                      String safeMessage,
                                      Throwable cause) {
        super(safeMessage, cause);
        this.serviceName = serviceName;
        this.operation = operation;
        this.statusCode = statusCode;
        this.safeMessage = safeMessage;
    }

    public String getServiceName() { return serviceName; }
    public String getOperation() { return operation; }
    public int getStatusCode() { return statusCode; }
    public String getSafeMessage() { return safeMessage; }
}
