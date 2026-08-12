package com.bankstack.rag.mapping;

import com.bankstack.rag.model.Chunk;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * ChunkDocumentMapper bridges our domain Chunk model
 * to storage-oriented representations.
 *
 * It supports:
 * 1. Chunk -> Spring AI Document (for PgVectorStore)
 * 2. Chunk metadata -> JSON string (for rag_keyword_index JSONB column)
 */
@Component
public class ChunkDocumentMapper {

    private final ObjectMapper objectMapper;

    public ChunkDocumentMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Document toDocument(Chunk chunk) {
        return new Document(chunk.chunkId(), chunk.text(), toMetadataMap(chunk));
    }

    public Map<String, Object> toMetadataMap(Chunk chunk) {
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("chunkId", chunk.chunkId());
        metadata.put("docId", chunk.document().docId());
        metadata.put("docType", chunk.document().docType());
        metadata.put("version", chunk.document().version());
        metadata.put("title", chunk.document().title());
        metadata.put("sectionPath", chunk.sectionPath());
        metadata.put("ordinal", chunk.ordinal());
        metadata.put("chunkHash", chunk.chunkHash());
        metadata.put("sensitivity", chunk.sensitivity().name());
        metadata.put("policyTags", join(chunk.policyTags()));
        metadata.put("allowedScopes", join(chunk.allowedScopes()));

        if (chunk.document().effectiveFrom() != null) {
            metadata.put("effectiveFrom", chunk.document().effectiveFrom().toString());
        }
        if (chunk.document().effectiveTo() != null) {
            metadata.put("effectiveTo", chunk.document().effectiveTo().toString());
        }

        return metadata;
    }

    public String toMetadataJson(Chunk chunk) {
        try {
            return objectMapper.writeValueAsString(toMetadataMap(chunk));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize chunk metadata to JSON", e);
        }
    }

    private String join(Iterable<String> values) {
        StringJoiner joiner = new StringJoiner(",");
        for (String value : values) {
            joiner.add(value);
        }
        return joiner.toString();
    }
}