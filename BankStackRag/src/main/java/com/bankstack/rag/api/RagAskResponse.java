package com.bankstack.rag.api;

import com.bankstack.rag.prompt.TaskType;
import com.bankstack.rag.verify.CitationCheckResult;

import java.util.List;

/**
 * RagAskResponse is the final API output of the end-to-end RAG pipeline.
 *
 * It includes:
 * - final answer text
 * - classified task type
 * - whether citations fully verified
 * - verification details
 * - rendered evidence block for observability / teaching
 *
 * Why expose renderedContext?
 * For a learning project and Udemy course, this is extremely useful
 * to show exactly what evidence reached the model.
 */
public record RagAskResponse(
        String answer,
        TaskType taskType,
        boolean fullyVerified,
        List<CitationCheckResult> citationChecks,
        String renderedContext,
        String effectiveConversationId
) {
}