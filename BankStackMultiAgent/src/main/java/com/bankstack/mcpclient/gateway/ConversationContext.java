package com.bankstack.mcpclient.gateway;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ConversationContext(
        String conversationId,
        Map<String, Object> entities,
        Instant lastUpdatedAt,

        PreparedActionType preparedActionType,
        Instant preparedAt,
        String lastPreparedSummary,
        String confirmationToken,
        Map<String, Object> preparedActionData,

        String awaitingTool,
        List<String> missingFields,
        Map<String, Object> awaitingArgumentData,
        
        boolean awaitingRecoveredContextConfirmation,
        Map<String, Object> suggestedRecoveredArguments
) {

    public static ConversationContext empty() {
        return new ConversationContext(
                null,
                Map.of(),
                null,
                PreparedActionType.NONE,
                null,
                null,
                null,
                Map.of(),
                null,
                List.of(),
                Map.of(),
                false,
                Map.of()
        );
    }

    public boolean hasPreparedAction() {
        return preparedActionType != null
                && preparedActionType != PreparedActionType.NONE
                && confirmationToken != null
                && !confirmationToken.isBlank();
    }

    public boolean isAwaitingInput() {
        return awaitingTool != null
                && !awaitingTool.isBlank()
                && missingFields != null
                && !missingFields.isEmpty();
    }
    
    public boolean hasRecoveredSuggestions() {
        return suggestedRecoveredArguments != null
                && !suggestedRecoveredArguments.isEmpty();
    }
}