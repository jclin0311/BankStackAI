package com.bankstack.mcpclient.config;

import com.commons.security.token.SecurityTokenProvider;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

@Configuration
public class McpClientConfig {

    private static final String AUTHORIZATION_CONTEXT_KEY = "authorization";

    @Bean
    McpSyncClientCustomizer bankstackClientCustomizer(SecurityTokenProvider tokenProvider) {
        return (serverConfigurationName, spec) -> {
            if (!"bankstack-server".equals(serverConfigurationName)) {
                return;
            }
            spec.requestTimeout(Duration.ofSeconds(30));
            spec.transportContextProvider(() -> {
                String token = resolveBearerToken(tokenProvider);
                if (token == null || token.isBlank()) {
                    return McpTransportContext.create(Map.of());
                }
                return McpTransportContext.create(
                        Map.of(AUTHORIZATION_CONTEXT_KEY, "Bearer " + token)
                );
            });
        };
    }

    @Bean
    McpSyncHttpClientRequestCustomizer mcpSyncHttpClientRequestCustomizer(
            McpServerConnectionProperties properties
    ) {
        return (builder, method, endpoint, body, context) -> {
            String configuredBaseUrl = properties.baseUrl();
            if (configuredBaseUrl == null || configuredBaseUrl.isBlank() || endpoint == null) {
                return;
            }

            var configuredUri = java.net.URI.create(configuredBaseUrl);
            boolean sameScheme = equalsSafe(configuredUri.getScheme(), endpoint.getScheme());
            boolean sameHost = equalsSafe(configuredUri.getHost(), endpoint.getHost());
            boolean samePort = configuredUri.getPort() == endpoint.getPort();

            if (!sameScheme || !sameHost || !samePort) {
                return;
            }

            Object authorization = context.get(AUTHORIZATION_CONTEXT_KEY);
            if (authorization != null) {
                builder.header("Authorization", authorization.toString());
            }
        };
    }

    private String resolveBearerToken(SecurityTokenProvider tokenProvider) {
        try {
            return tokenProvider.currentBearerToken();
        } catch (RuntimeException ex) {
            /*
             * MCP clients may perform startup discovery before an HTTP user request
             * exists. In that case there is no SecurityContext yet, so we return
             * an empty transport context. Real tool calls made during an authenticated
             * user request will still relay the user's bearer token.
             */
            return null;
        }
    }

    private boolean equalsSafe(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equalsIgnoreCase(right);
    }
}
