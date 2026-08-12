package com.bankstack.mcp.confirmation;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ConfirmationTokenGenerator {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}