package com.bankstack.rag.verify;

/**
 * CitationCheckResult stores the verification outcome for one citation.
 *
 * Fields:
 * - referenceNumber: cited snippet number, e.g. [1]
 * - valid: whether verification passed
 * - reason: explanation for pass/fail
 *
 * Example reasons:
 * - "Reference exists and lexical support is sufficient"
 * - "Reference number not found in assembled context"
 * - "Claim sentence has weak overlap with cited snippet"
 */
public record CitationCheckResult(
        int referenceNumber,
        boolean valid,
        String reason
) {
}