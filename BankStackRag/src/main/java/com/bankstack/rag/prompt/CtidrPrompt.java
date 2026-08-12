package com.bankstack.rag.prompt;

/**
 * CtidrPrompt represents the final prompt structure built
 * from the CTIDR framework.
 *
 * Components:
 * - systemPrompt → instructions for model behavior
 * - userPrompt → user question
 * - contextBlock → retrieved evidence
 */
public record CtidrPrompt(
        String systemPrompt,
        String contextBlock,
        String userPrompt
) {
}