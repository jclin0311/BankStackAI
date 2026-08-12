package com.bankstack.rag.api;

/**
 * The request carries user intent. Authorization is derived from the validated JWT.
 */
public record RagAskRequest(
        String query,
        String conversationId
) {}
