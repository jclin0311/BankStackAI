package com.bankstack.mcpclient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bankstack.gateway.conversation")
public record GatewayConversationProperties(
        long preparedActionTtlSeconds
) {
}