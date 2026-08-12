package com.commons.exception;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String customerId) {
        super("No customer with ID: " + customerId);
    }
}
