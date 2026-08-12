package com.bankstack.mcpclient.memory;

import java.util.Map;

public record RecoveredContextSuggestion(
        boolean found,
        Map<String, Object> recoveredArguments,
        String message
) {
    public static RecoveredContextSuggestion none() {
        return new RecoveredContextSuggestion(false, Map.of(), null);
    }
}