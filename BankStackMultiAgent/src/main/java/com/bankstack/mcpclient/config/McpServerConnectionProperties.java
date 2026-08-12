package com.bankstack.mcpclient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bankstack.mcp.server")
public record McpServerConnectionProperties(
        String baseUrl
) {
}