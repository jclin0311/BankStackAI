package com.bankstack.mcpclient.memory;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ContextRecoveryService {

    private final ActionMemoryService actionMemoryService;

    public ContextRecoveryService(ActionMemoryService actionMemoryService) {
        this.actionMemoryService = actionMemoryService;
    }

    public RecoveredContextSuggestion recover(
            String actorId,
            String toolName,
            Map<String, Object> currentArguments
    ) {

        Optional<Map<String, Object>> memory =
                actionMemoryService.findLatestSuccessfulArguments(
                        actorId,
                        toolName
                );

        if (memory.isEmpty()) {
            return RecoveredContextSuggestion.none();
        }

        Map<String, Object> recovered = new HashMap<>();

        Map<String, Object> historicalArguments = memory.get();

        recoverField(
                recovered,
                currentArguments,
                historicalArguments,
                "debtorAccountId"
        );

        recoverField(
                recovered,
                currentArguments,
                historicalArguments,
                "billerReferenceNumber"
        );

        recoverField(
                recovered,
                currentArguments,
                historicalArguments,
                "currency"
        );

        if (recovered.isEmpty()) {
            return RecoveredContextSuggestion.none();
        }

        return new RecoveredContextSuggestion(
                true,
                recovered,
                buildSuggestionMessage(recovered)
        );
    }

    private void recoverField(Map<String, Object> target,
                              Map<String, Object> currentArguments,
                              Map<String, Object> historicalArguments,
                              String field) {

        Object currentValue = currentArguments.get(field);

        if (!isBlank(currentValue)) {
            return;
        }

        Object historicalValue = historicalArguments.get(field);

        if (isBlank(historicalValue)) {
            return;
        }

        target.put(field, historicalValue);
    }

    private String buildSuggestionMessage(Map<String, Object> recovered) {

        String account =
                maskAccount(
                        recovered.get("debtorAccountId")
                );

        String billerReference =
                stringValue(recovered.get("billerReferenceNumber"));

        String currency =
                stringValue(recovered.get("currency"));

        return """
                I found your previous Hydro bill setup:

                - Account: %s
                - Biller Reference: %s
                - Currency: %s

                Would you like me to use these details?
                """.formatted(
                account,
                billerReference,
                currency
        );
    }

    private String maskAccount(Object value) {

        String text = stringValue(value);

        if (text == null || text.length() < 4) {
            return "****";
        }

        return "****" + text.substring(text.length() - 4);
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean isBlank(Object value) {
        return value == null || value.toString().trim().isEmpty();
    }
}