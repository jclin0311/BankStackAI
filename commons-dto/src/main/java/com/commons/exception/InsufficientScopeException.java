package com.commons.exception;

public class InsufficientScopeException extends RuntimeException {

    private final String requiredScope;

    public InsufficientScopeException(String requiredScope) {
        super("Required permission is missing: " + requiredScope);
        this.requiredScope = requiredScope;
    }

    public String getRequiredScope() {
        return requiredScope;
    }
}
