package com.commons.exception;

public class DownstreamNotFoundException extends DownstreamServiceException {
    public DownstreamNotFoundException(String serviceName, String operation, String safeMessage) {
        super(serviceName, operation, 404, safeMessage);
    }
}
