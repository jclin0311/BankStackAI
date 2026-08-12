package com.bankstack.rag.api;

import com.bankstack.rag.ingest.DocumentIngestService;
import com.bankstack.rag.ingest.DocumentInput;
import com.bankstack.rag.model.Chunk;
import com.bankstack.rag.model.DocumentRef;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RagIngestController exposes document ingestion over HTTP.
 *
 * Endpoint:
 * POST /api/rag/ingest
 *
 * This controller takes raw document input,
 * builds the domain-level DocumentRef and DocumentInput,
 * and passes them into the ingestion pipeline.
 *
 * The ingestion pipeline then:
 * - chunks the document
 * - enriches metadata
 * - writes to pgvector semantic index
 * - writes to tsvector lexical index
 */
@RestController
@RequestMapping("/api/rag")
public class RagIngestController {

    private final DocumentIngestService documentIngestService;

    public RagIngestController(DocumentIngestService documentIngestService) {
        this.documentIngestService = documentIngestService;
    }

    @PostMapping("/ingest")
    public RagIngestResponse ingest( @RequestBody RagIngestRequest request) {

        DocumentRef ref = new DocumentRef(
                request.docId(),
                request.docType(),
                request.version(),
                request.title(),
                request.effectiveFrom(),
                request.effectiveTo()
        );

        DocumentInput input = new DocumentInput(
        	    ref,
        	    request.text(),
        	    request.policyTags(),
        	    request.allowedScopes(),
        	    request.sensitivity()
        	);

        List<Chunk> chunks = documentIngestService.ingest(input);

        return new RagIngestResponse(
                request.docId(),
                request.version(),
                chunks.size(),
                "Document ingested successfully"
        );
    }
}