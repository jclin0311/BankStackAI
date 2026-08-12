package com.bankstack.rag.prompt;

/**
 * TaskType represents the high-level intent of the user query.
 *
 * Why this exists:
 * Different tasks require different response styles.
 *
 * For example:
 * POLICY_LOOKUP → explain rules with citations
 * PROCEDURE_LOOKUP → step-by-step instructions
 * GENERAL_QUESTION → explanatory answer
 *
 * For now we keep it simple.
 */
public enum TaskType {

    POLICY_LOOKUP,
    REFUSAL
}