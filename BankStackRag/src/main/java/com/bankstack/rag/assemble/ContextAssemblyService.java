package com.bankstack.rag.assemble;

import com.bankstack.rag.retrieve.HybridSearchResult;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ContextAssemblyService converts authorized retrieval results
 * into a compact, deduplicated, token-budget-aware evidence block.
 *
 * Why this service exists:
 * A good retrieval system can still produce a bad answer
 * if the LLM is given noisy, duplicated, or oversized context.
 *
 * This service solves that packaging problem.
 */
@Service
public class ContextAssemblyService {

    public AssembledContext assemble(List<HybridSearchResult> rankedResults,
                                     ContextAssemblyOptions options) {

        if (rankedResults == null || rankedResults.isEmpty()) {
            return new AssembledContext(List.of(), "", 0);
        }

        List<ContextSnippet> snippets = new ArrayList<>();
        Set<String> seenChunkIds = new HashSet<>();
        Set<String> seenNormalizedTexts = new HashSet<>();

        int referenceNumber = 1;
        int totalEstimatedTokens = 0;

        for (HybridSearchResult result : rankedResults) {

            if (snippets.size() >= options.maxSnippets()) {
                break;
            }

            if (isDuplicate(result, seenChunkIds, seenNormalizedTexts)) {
                continue;
            }

            String trimmedText = trimToMaxChars(result.text(), options.maxCharactersPerSnippet());
            int estimatedTokens = estimateTokens(trimmedText);

            if (totalEstimatedTokens + estimatedTokens > options.maxEstimatedTokens()) {
                continue;
            }

            ContextSnippet snippet = new ContextSnippet(
                    referenceNumber++,
                    result.chunkId(),
                    trimmedText,
                    result.metadata()
            );

            snippets.add(snippet);
            totalEstimatedTokens += estimatedTokens;

            seenChunkIds.add(result.chunkId());
            seenNormalizedTexts.add(normalizeText(result.text()));
        }

        String rendered = render(snippets);

        return new AssembledContext(
                snippets,
                rendered,
                totalEstimatedTokens
        );
    }

    /**
     * Duplicate logic:
     * - same chunkId
     * - or same normalized text
     *
     * This is intentionally simple and explainable.
     */
    private boolean isDuplicate(HybridSearchResult result,
                                Set<String> seenChunkIds,
                                Set<String> seenNormalizedTexts) {

        if (result.chunkId() != null && seenChunkIds.contains(result.chunkId())) {
            return true;
        }

        String normalized = normalizeText(result.text());
        return seenNormalizedTexts.contains(normalized);
    }

    /**
     * Renders the selected snippets into a prompt-friendly block.
     *
     * Example:
     * [1] Source: Security Policy | Section: Token Relay | Chunk: abc123
     * Token relay forwards the user's JWT...
     */
    private String render(List<ContextSnippet> snippets) {
        StringBuilder sb = new StringBuilder();

        for (ContextSnippet snippet : snippets) {
            Map<String, Object> metadata = snippet.metadata();

            String title = read(metadata, "title");
            String sectionPath = read(metadata, "sectionPath");
            String version = read(metadata, "version");

            sb.append("[")
              .append(snippet.referenceNumber())
              .append("] Source: ")
              .append(fallback(title, "Unknown Title"));

            if (version != null && !version.isBlank()) {
                sb.append(" | Version: ").append(version);
            }

            if (sectionPath != null && !sectionPath.isBlank()) {
                sb.append(" | Section: ").append(sectionPath);
            }

            sb.append(" | Chunk: ").append(snippet.chunkId()).append("\n");
            sb.append(snippet.text()).append("\n\n");
        }

        return sb.toString().trim();
    }

    private String fallback(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String read(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get(key);
        return value == null ? null : value.toString();
    }

    /**
     * Hard trims a snippet to the configured maximum size.
     */
    private String trimToMaxChars(String text, int maxChars) {
        if (text == null) {
            return "";
        }

        String trimmed = text.strip();
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }

        return trimmed.substring(0, maxChars).strip() + " ...";
    }

    /**
     * Very rough token estimate.
     *
     * Rule of thumb:
     * ~ 1 token ~= 4 characters
     */
    int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    /**
     * Normalizes text for duplicate detection.
     */
    String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }
}