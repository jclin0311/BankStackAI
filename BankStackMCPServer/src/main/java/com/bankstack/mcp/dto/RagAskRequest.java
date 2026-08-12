package com.bankstack.mcp.dto;

public record RagAskRequest(
        String query,
        String conversationId
) {}
