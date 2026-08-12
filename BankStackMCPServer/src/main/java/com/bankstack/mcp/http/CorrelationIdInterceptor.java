package com.bankstack.mcp.http;

import org.slf4j.MDC;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.UUID;

public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    public static final String HEADER_NAME = "X-Request-Id";

    @Override
    public ClientHttpResponse intercept(
            org.springframework.http.HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        String cid = MDC.get("cid");
        if (cid == null || cid.isBlank()) {
            cid = UUID.randomUUID().toString();
        }

        request.getHeaders().set(HEADER_NAME, cid);
        return execution.execute(request, body);
    }
}