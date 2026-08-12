package com.bankstack.rag.chunking;

/**
 * RawChunk represents the output of the structural chunker
 * BEFORE policy metadata is applied.
 *
 * Why we keep this intermediate representation:
 *
 * Chunking and policy enrichment are separate concerns.
 *
 * Step 1: Structural chunker splits document text
 * Step 2: Metadata resolver attaches scopes/tags/sensitivity
 *
 * This separation keeps the ingestion pipeline modular.
 */
public record RawChunk(
        String sectionPath,
        int ordinal,
        String text
) {}