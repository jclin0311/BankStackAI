package com.bankstack.mcpclient.gateway;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Route-level safety guard.
 *
 * This guard is not a prompt-injection detector. Prompt manipulation is handled
 * before route classification by PromptInjectionGuard.
 *
 * This class only detects unsafe banking intent that should route to REFUSE.
 * It does not perform actor validation, ownership checks, or role/scope
 * enforcement. Those remain the responsibility of BankStackMCPServer.
 */
@Component
public class SafetyRouteGuard {

    private static final List<String> UNSAFE_BANKING_INTENTS = List.of(
            "bypass bank authentication",
            "bypass authentication",
            "bypass login",
            "bypass mfa",
            "bypass two factor",
            "bypass 2fa",
            "access another customer's account",
            "access someone else's account",
            "show another customer's account",
            "show someone else's account",
            "show other customer's account",
            "show other user's account",
            "give me all accounts",
            "list all customer accounts",
            "transfer from someone else's account",
            "pay from someone else's account",
            "delete audit logs",
            "erase audit logs",
            "disable fraud checks",
            "disable fraud monitoring",
            "fake jwt",
            "forge jwt",
            "forge token",
            "admin token",
            "steal money",
            "hack account",
            "launder money"
    );

    public boolean isUnsafe(String message) {
        String normalized = normalize(message);
        if (normalized.isBlank()) {
            return false;
        }

        return containsAny(normalized, UNSAFE_BANKING_INTENTS);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private boolean containsAny(String value, List<String> terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
