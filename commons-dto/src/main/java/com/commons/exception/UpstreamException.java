package com.commons.exception;
public class UpstreamException extends RuntimeException {
    public UpstreamException(String message) { super(message); }
    public UpstreamException(String message, Throwable cause) { super(message, cause); }
}
