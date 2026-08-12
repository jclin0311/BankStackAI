package com.bankstack.rag.retrieve;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * KeywordSearchService performs lexical retrieval using PostgreSQL full-text search.
 */
@Service
public class KeywordSearchService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public KeywordSearchService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<KeywordSearchResult> search(String query, int topK) {

        String sql = """
                SELECT
                    chunk_id,
                    content,
                    metadata,
                    ts_rank(
                        search_vector,
                        websearch_to_tsquery('english', ?)
                    ) AS keyword_score
                FROM rag_keyword_index
                WHERE search_vector @@ websearch_to_tsquery('english', ?)
                ORDER BY keyword_score DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, ps -> {
            ps.setString(1, query);
            ps.setString(2, query);
            ps.setInt(3, topK);
        }, (rs, rowNum) -> {

            String chunkId = rs.getString("chunk_id");
            String content = rs.getString("content");
            String metadataJson = rs.getString("metadata");
            double keywordScore = rs.getDouble("keyword_score");

            return new KeywordSearchResult(
                    chunkId,
                    content,
                    parseMetadata(metadataJson),
                    keywordScore
            );
        });
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        try {
            return objectMapper.readValue(
                    metadataJson,
                    new TypeReference<>() {}
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse metadata JSON",
                    e
            );
        }
    }
}