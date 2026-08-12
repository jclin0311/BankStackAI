package com.bankstack.rag.ingest;

import java.util.List;

import com.bankstack.rag.model.DocumentRef;
import com.bankstack.rag.model.Sensitivity;

/**
 * DocumentInput represents a document entering the ingestion pipeline.
 *
 * We keep document metadata separate from the document text.

 */
public record DocumentInput(
		DocumentRef ref,
	    String text,
	    List<String> policyTags,
	    List<String> allowedScopes,
	    Sensitivity sensitivity
) {}