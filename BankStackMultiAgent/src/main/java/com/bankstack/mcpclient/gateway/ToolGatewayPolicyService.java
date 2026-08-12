package com.bankstack.mcpclient.gateway;

import org.springframework.stereotype.Component;

@Component
public class ToolGatewayPolicyService {

    public boolean shouldMarkAsPrepared(String response) {
        if (response == null || response.isBlank()) {
            return false;
        }

        String normalized = response.toLowerCase();

        return normalized.contains("confirmation token")
                || normalized.contains("prepared")
                || normalized.contains("please confirm")
                || normalized.contains("confirm this payment");
    }

    public String summarizePreparedAction(String response) {
        if (response == null || response.isBlank()) {
            return "Prepared payment action";
        }

        String trimmed = response.trim();
        return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 300);
    }
}