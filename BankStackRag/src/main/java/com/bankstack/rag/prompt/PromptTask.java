package com.bankstack.rag.prompt;

/**
 * PromptTask describes the user task that the model must perform.
 *
 * This is separated from raw query because
 * the system may later classify or rewrite the query.
 */
public record PromptTask(
        TaskType type,
        String userQuery
) {
}