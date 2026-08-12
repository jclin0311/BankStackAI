package com.bankstack.mcp.api;

import java.util.Map;

public record ToolInvokeRequest(
        String tool,
        Map<String, Object> input
) {
}