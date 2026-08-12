package com.bankstack.rag.api;

import java.time.Instant;
import java.util.List;

import com.bankstack.rag.model.Sensitivity;

/**
 * RagIngestRequest is the API input for document ingestion.
 *
 * This endpoint is used to push source documents into the RAG knowledge base.
 *
 * Required:
 * - docId
 * - docType
 * - version
 * - title
 * - text
 *
 * Optional:
 * - effectiveFrom
 * - effectiveTo

 */
public record RagIngestRequest(
        String docId,

        String docType,

        String version,

        String title,

        Instant effectiveFrom,
        Instant effectiveTo,

        String text,
        
        List<String> policyTags,
        List<String> allowedScopes,
        Sensitivity sensitivity
) {
}