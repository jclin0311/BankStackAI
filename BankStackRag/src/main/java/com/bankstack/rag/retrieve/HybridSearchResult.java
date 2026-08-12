package com.bankstack.rag.retrieve;

import java.util.Map;

/**
 * Merged result of semantic + lexical retrieval.
 */
public record HybridSearchResult(
        String chunkId,
        String text,
        Map<String, Object> metadata,
        double vectorScore,
        double keywordScore,
        double finalScore
) {
}