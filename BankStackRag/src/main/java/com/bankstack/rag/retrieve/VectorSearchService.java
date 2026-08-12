package com.bankstack.rag.retrieve;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Semantic retrieval service backed by Spring AI VectorStore.
 */
@Slf4j
@Service
public class VectorSearchService {

    private final VectorStore vectorStore;

    public VectorSearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<RagSearchResult> search(String query, int topK) {
        return searchWithScores(query, topK).stream()
                .map(r -> new RagSearchResult(r.chunkId(), r.text(), r.metadata()))
                .toList();
    }



    
    public List<SemanticSearchResult> searchWithScores(String query, int topK) {
        var builder = SearchRequest.builder()
                .query(query)
                .topK(topK);

       
        List<Document> documents = vectorStore.similaritySearch(builder.build());
        List<SemanticSearchResult> results = new ArrayList<>();

        if (documents == null || documents.isEmpty()) {
            log.warn("Vector search returned no documents for query='{}'", query);
            return results;
        }

        for (Document doc : documents) {
            Map<String, Object> metadata = doc.getMetadata();
            double vectorScore = extractScore(metadata);

            log.info("Vector result. chunkId={}, vectorScore={}, metadata={}",
                    doc.getId(), vectorScore, metadata);

            results.add(new SemanticSearchResult(
                    doc.getId(),
                    doc.getText(),
                    metadata,
                    vectorScore
            ));
        }

        return results;
    }

   
    private double extractScore(Map<String, Object> metadata) {
        if (metadata == null) {
            return 0.0;
        }

        Object similarity = metadata.get("similarity");
        if (similarity != null) {
            return parseNumber(similarity);
        }

        Object score = metadata.get("score");
        if (score != null) {
            return parseNumber(score);
        }

        Object distance = metadata.get("distance");
        if (distance != null) {
            double parsedDistance = parseNumber(distance);

            return Math.max(
                    0.0,
                    Math.min(1.0, 1.0 - parsedDistance)
            );
        }

        return 0.0;
    }

    private double parseNumber(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }

        return 0.0;
    }
    
}