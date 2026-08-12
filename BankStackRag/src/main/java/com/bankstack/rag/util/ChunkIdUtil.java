package com.bankstack.rag.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Deterministic chunk IDs:
 * Same docId + version + sectionPath + ordinal + chunkHash => same chunkId always.
 */
public final class ChunkIdUtil {
    private ChunkIdUtil() {}

    public static String deterministicChunkId(String docId, String version, String sectionPath, int ordinal, String chunkHash) {
        String key = docId + "|" + version + "|" + sectionPath + "|" + ordinal + "|" + chunkHash;
        UUID uuid = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        return uuid.toString();
    }
}