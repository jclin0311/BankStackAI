package com.bankstack.rag.retrieve;

import com.bankstack.rag.rerank.LocalRerankerService;
import com.bankstack.rag.security.AccessContext;
import com.bankstack.rag.security.PermissionFilterService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HybridSearchService {

	private static final double VECTOR_WEIGHT = 0.7;
	private static final double KEYWORD_WEIGHT = 0.3;

	private final VectorSearchService vectorSearchService;
	private final KeywordSearchService keywordSearchService;
	private final PermissionFilterService permissionFilterService;
	private final LocalRerankerService localRerankerService;
	private final PolicyValidityFilterService policyValidityFilterService;

	public HybridSearchService(VectorSearchService vectorSearchService, KeywordSearchService keywordSearchService,
			PermissionFilterService permissionFilterService, LocalRerankerService localRerankerService,
			PolicyValidityFilterService policyValidityFilterService) {
		this.vectorSearchService = vectorSearchService;
		this.keywordSearchService = keywordSearchService;
		this.permissionFilterService = permissionFilterService;
		this.localRerankerService = localRerankerService;
		this.policyValidityFilterService = policyValidityFilterService;
	}

	public List<HybridSearchResult> search(String query, int topK, AccessContext accessContext) {
		int fetchK = Math.max(topK * 3, 12);

		Map<String, MergeCandidate> merged = new LinkedHashMap<>();

		List<SemanticSearchResult> semanticResults = vectorSearchService.searchWithScores(query, fetchK);

		List<KeywordSearchResult> keywordResults = keywordSearchService.search(query, fetchK);

		mergeSemanticResults(merged, semanticResults);
		mergeKeywordResults(merged, keywordResults);

		List<HybridSearchResult> candidates = new ArrayList<>();

		for (MergeCandidate c : merged.values()) {
			double finalScore = score(c.vectorScore, c.keywordScore);
			candidates.add(
					new HybridSearchResult(c.chunkId, c.text, c.metadata, c.vectorScore, c.keywordScore, finalScore));
		}

		/*
		 * Retrieval may find relevant chunks from expired or future-dated policies.
		 * Those chunks must not become evidence.
		 *
		 * Order matters: 1. Permission filter removes unauthorized chunks. 2. Validity
		 * filter removes expired/future chunks. 3. Reranker selects the best evidence
		 * from safe + current candidates.
		 */
		List<HybridSearchResult> authorized = permissionFilterService.filter(candidates, accessContext);
		List<HybridSearchResult> current = policyValidityFilterService.filterCurrent(authorized);

		return localRerankerService.rerank(query, current, topK);
	}

	private void mergeSemanticResults(Map<String, MergeCandidate> merged, List<SemanticSearchResult> semanticResults) {
		for (SemanticSearchResult semantic : semanticResults) {
			MergeCandidate candidate = merged.computeIfAbsent(semantic.chunkId(),
					id -> new MergeCandidate(semantic.chunkId(), semantic.text(), semantic.metadata()));
			candidate.vectorScore = Math.max(candidate.vectorScore, semantic.vectorScore());
			if ((candidate.metadata == null || candidate.metadata.isEmpty()) && semantic.metadata() != null) {
				candidate.metadata = semantic.metadata();
			}
		}
	}

	private void mergeKeywordResults(Map<String, MergeCandidate> merged, List<KeywordSearchResult> keywordResults) {
		for (KeywordSearchResult keyword : keywordResults) {
			MergeCandidate candidate = merged.computeIfAbsent(keyword.chunkId(),
					id -> new MergeCandidate(keyword.chunkId(), keyword.text(), keyword.metadata()));
			candidate.keywordScore = Math.max(candidate.keywordScore, keyword.keywordScore());

			if ((candidate.metadata == null || candidate.metadata.isEmpty()) && keyword.metadata() != null) {
				candidate.metadata = keyword.metadata();
			}
		}
	}

	private double score(double vectorScore, double keywordScore) {
		return (vectorScore * VECTOR_WEIGHT) + (keywordScore * KEYWORD_WEIGHT);
	}

	private static class MergeCandidate {
		private final String chunkId;
		private final String text;
		private Map<String, Object> metadata;
		private double vectorScore;
		private double keywordScore;

		private MergeCandidate(String chunkId, String text, Map<String, Object> metadata) {
			this.chunkId = chunkId;
			this.text = text;
			this.metadata = metadata;
		}
	}
}
