package com.bankstack.rag.chunking;

import java.util.List;

/**
 * StructuralChunker is an abstraction for "how we split a document into chunks".
 *
 * Why interface?
 *  - Today we implement MarkdownStructuralChunker (based on headings)
 *  - Tomorrow you may ingest:
 *      - PDFs
 *      - HTML pages
 *      - Confluence exports
 *      - JSON policies
 *    and each needs a different structural chunking strategy.
 *
 * The ingestion pipeline should NOT change when chunking strategy changes.
 * It will depend only on this interface.
 */
public interface StructuralChunker {

    /**
     * Splits raw input text into ordered chunks.
     *
     * Output guarantees:
     *  - Each RawChunk has a sectionPath (for citations)
     *  - Each RawChunk has an ordinal (stable order inside the document)
     *  - Chunk text is the content under that section
     */
    List<RawChunk> chunk(String inputText);
}