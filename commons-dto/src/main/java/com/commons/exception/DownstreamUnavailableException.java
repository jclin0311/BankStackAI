package com.commons.exception;

public class DownstreamUnavailableException extends DownstreamServiceException {
    public DownstreamUnavailableException(String serviceName, String operation, String safeMessage, Throwable cause) {
        super(serviceName, operation, 503, safeMessage, cause);
    }
    public DownstreamUnavailableException(String serviceName, String operation, String safeMessage) {
        super(serviceName, operation, 503, safeMessage);
    }
}
