package com.bankstack.mcpclient.gateway;

import com.bankstack.mcpclient.api.GatewayChatRequest;
import org.springframework.stereotype.Component;

@Component
public class GatewayRequestValidator {

    public void validate(GatewayChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }

        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("Message is required.");
        }

        if (request.message().length() > 4000) {
            throw new IllegalArgumentException("Message is too long.");
        }
    }
}
