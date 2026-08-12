package com.bankstack.rag.retrieve;

import java.util.Map;

/**
 * RagSearchResult is the retrieval-facing result returned by our RAG layer.
 *
 * Why not expose Spring AI Document directly?
 * Because upper layers of the application should depend on our domain language,
 * not the storage provider's object model.
 *
 * This keeps the architecture clean and makes future refactors easier.
 */
public record RagSearchResult(
        String chunkId,
        String text,
        Map<String, Object> metadata
) {
}