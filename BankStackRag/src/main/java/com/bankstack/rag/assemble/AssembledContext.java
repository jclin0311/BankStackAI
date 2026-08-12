package com.bankstack.rag.assemble;

import java.util.List;

/**
 * AssembledContext is the final output of the context assembly stage.
 *
 * It contains:
 * - selected snippets
 * - rendered text block ready to place into a prompt
 * - total estimated token usage
 *
 * Why this matters:
 * Retrieval gives us candidate evidence.
 * Context assembly transforms that evidence into something
 * compact, deduplicated, and LLM-friendly.
 */
public record AssembledContext(
        List<ContextSnippet> snippets,
        String renderedContext,
        int estimatedTokenCount
) {
}