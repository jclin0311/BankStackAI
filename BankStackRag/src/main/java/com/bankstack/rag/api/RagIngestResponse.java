package com.bankstack.rag.api;

/**
 * RagIngestResponse summarizes the ingestion result.
 *
 * We return high-level ingestion visibility:
 * - which document was ingested
 * - which version
 * - how many chunks were created
 *
 * This is helpful for:
 * - Postman testing
 * - operational visibility
 * - Udemy demos
 */
public record RagIngestResponse(
        String docId,
        String version,
        int chunkCount,
        String message
) {
}