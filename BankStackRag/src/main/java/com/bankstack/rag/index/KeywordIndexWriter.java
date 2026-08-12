package com.bankstack.rag.index;

import com.bankstack.rag.model.Chunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Types;

/**
 * KeywordIndexWriter persists chunks into our PostgreSQL full-text-search table.
 *
 * Why this class exists:
 * Semantic retrieval and lexical retrieval are two separate concerns.
 *
 * - PgVectorStore handles embeddings + semantic search
 * - rag_keyword_index handles exact / lexical / full-text search
 *
 * During ingestion, every Chunk is written to both systems.
 */
@Component
public class KeywordIndexWriter {

    private final JdbcTemplate jdbcTemplate;

    public KeywordIndexWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Upserts one chunk into the lexical keyword index.
     *
     * We build the tsvector using PostgreSQL's to_tsvector('english', ...).
     * The english config provides stemming and stop-word handling.
     */
    public void upsert(Chunk chunk, String metadataJson) {
        String sql = """
                INSERT INTO rag_keyword_index
                    (chunk_id, doc_id, version, chunk_hash, content, metadata, search_vector)
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), to_tsvector('english', ?))
                ON CONFLICT (chunk_id)
                DO UPDATE SET
                    doc_id = EXCLUDED.doc_id,
                    version = EXCLUDED.version,
                    chunk_hash = EXCLUDED.chunk_hash,
                    content = EXCLUDED.content,
                    metadata = EXCLUDED.metadata,
                    search_vector = EXCLUDED.search_vector
                """;

        jdbcTemplate.update(
                sql,
                chunk.chunkId(),
                chunk.document().docId(),
                chunk.document().version(),
                chunk.chunkHash(),
                chunk.text(),
                metadataJson,
                chunk.text()
        );
    }
    
    
    
    public boolean existsByDocVersionAndHash(String docId, String version, String chunkHash) {
        String sql = """
                SELECT COUNT(1)
                FROM rag_keyword_index
                WHERE doc_id = ?
                  AND version = ?
                  AND chunk_hash = ?
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                docId,
                version,
                chunkHash
        );

        return count != null && count > 0;
    }
    
    
}