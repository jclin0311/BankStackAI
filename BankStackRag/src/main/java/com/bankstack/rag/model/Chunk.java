package com.bankstack.rag.model;

import java.util.List;

public record Chunk(
        String chunkId,
        DocumentRef document,
        String sectionPath,
        int ordinal,
        String text,
        String chunkHash,
        List<String> policyTags,
        List<String> allowedScopes,
        Sensitivity sensitivity
) {
}