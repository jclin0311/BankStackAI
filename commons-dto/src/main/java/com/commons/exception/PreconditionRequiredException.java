package com.commons.exception;
public class PreconditionRequiredException extends RuntimeException {
    public PreconditionRequiredException(String msg) { super(msg); }
}