package com.bankstack.rag.rerank;

import com.bankstack.rag.retrieve.HybridSearchResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lightweight deterministic reranker.
 *
 * Production idea:
 * Retrieval produces candidates. Reranking decides which evidence is strong
 * enough to send to the LLM.
 *
 * This is intentionally local and explainable for Udemy. Later you can swap
 * it for Cohere Rerank, BGE reranker, or another cross-encoder.
 */
@Service
public class LocalRerankerService {

    public List<HybridSearchResult> rerank(String query, List<HybridSearchResult> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        Set<String> queryTerms = importantTerms(query);

        List<HybridSearchResult> reranked = candidates.stream()
                .map(result -> withRerankScore(result, queryTerms))
                .sorted(Comparator.comparingDouble(HybridSearchResult::finalScore).reversed())
                .limit(topK)
                .toList();
        
        
                return reranked;
    }

    private HybridSearchResult withRerankScore(HybridSearchResult result, Set<String> queryTerms) {
        String text = result.text() == null ? "" : result.text().toLowerCase(Locale.ROOT);
       // Map<String, Object> metadata = result.metadata();
        //String section = metadata == null ? "" : String.valueOf(metadata.getOrDefault("sectionPath", "")).toLowerCase(Locale.ROOT);

        double score = result.finalScore();
        score += lexicalOverlapBoost(text, queryTerms);

        return new HybridSearchResult(
                result.chunkId(),
                result.text(),
                result.metadata(),
                result.vectorScore(),
                result.keywordScore(),
                score
        );
    }

    private double lexicalOverlapBoost(
            String text,
            Set<String> queryTerms) {

        if (queryTerms.isEmpty() || text.isBlank()) {
            return 0.0;
        }

        long matches = queryTerms.stream()
                .filter(text::contains)
                .count();

        double ratio = (double) matches / queryTerms.size();

        return ratio * 0.10;
    }
    
   
   

    private Set<String> importantTerms(String query) {
        if (query == null || query.isBlank()) {
            return Set.of();
        }

        Set<String> stopWords = Set.of(
                "what", "when", "where", "who", "why", "how",
                "the", "a", "an",
                "is", "are", "was", "were",
                "to", "of", "for", "in", "on", "during",
                "must", "should", "can", "does", "do",
                "customer", "customers",
                "policy", "policies",
                "please", "tell", "your"
        );

        return Arrays.stream(query.toLowerCase(Locale.ROOT).split("[^a-z0-9:_-]+"))
                .map(String::strip)
                .filter(term -> term.length() >= 3)
                .filter(term -> !stopWords.contains(term))
                .collect(Collectors.toCollection(HashSet::new));
    }
}
