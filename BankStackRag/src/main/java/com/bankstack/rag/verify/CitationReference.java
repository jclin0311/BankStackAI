package com.bankstack.rag.verify;

/**
 * CitationReference represents one citation mention found in the model answer.
 *
 * Example:
 * Answer sentence:
 *   "Token relay forwards JWT downstream [1]."
 *
 * We capture:
 * - referenceNumber = 1
 * - citedSentence = full sentence containing [1]
 *
 * Why sentence?
 * Because verification needs not only the citation number,
 * but also the claim text that is supposedly supported by that citation.
 */
public record CitationReference(
        int referenceNumber,
        String citedSentence
) {
}