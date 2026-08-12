package com.bankstack.mcp.dto;

public record PolicySnippet(
        String documentName,
        String excerpt,
        String citation
) {
}