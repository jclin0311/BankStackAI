package com.bankstack.rag.retrieve;

import java.util.Map;

/**
 * One semantic/vector retrieval result.
 */
public record SemanticSearchResult(
        String chunkId,
        String text,
        Map<String, Object> metadata,
        double vectorScore
) {
}