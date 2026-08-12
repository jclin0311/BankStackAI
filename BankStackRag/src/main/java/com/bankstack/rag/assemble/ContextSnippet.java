package com.bankstack.rag.assemble;

import java.util.Map;

/**
 * ContextSnippet represents one chunk that has been selected
 * for inclusion in the final LLM context.
 *
 * Why this class exists:
 * Retrieval results are about ranking.
 * Context snippets are about packaging evidence for model consumption.
 *
 * This object keeps:
 * - a stable reference id for citation-friendly formatting
 * - the chunk id
 * - the chunk text (possibly trimmed)
 * - metadata needed for source traceability
 */
public record ContextSnippet(
        int referenceNumber,
        String chunkId,
        String text,
        Map<String, Object> metadata
) {
}