package com.bankstack.rag.verify;

import java.util.List;

/**
 * VerifiedAnswer is the final output of the citation verification stage.
 *
 * It contains:
 * - original answer text
 * - all citation check results
 * - overall pass/fail signal
 *
 * Why this object matters:
 * Downstream API layers or UIs may choose to:
 * - show the answer only if verified
 * - show warning if partially supported
 * - reject unsupported answers
 */
public record VerifiedAnswer(
        String answer,
        List<CitationCheckResult> checks,
        boolean fullyVerified
) {
}