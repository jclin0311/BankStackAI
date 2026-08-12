package com.bankstack.rag.retrieve;

import java.util.Map;

/**
 * KeywordSearchResult represents one hit returned by keyword-based retrieval.
 *
 * Why a separate result type?
 * Because keyword search and vector search are different retrieval strategies.
 * Each may have its own score and tuning logic.
 *
 * We keep them separate first, then merge them in HybridSearchService.
 */
public record KeywordSearchResult(
        String chunkId,
        String text,
        Map<String, Object> metadata,
        double keywordScore
) {
}