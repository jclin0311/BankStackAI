package com.bankstack.mcpclient.gateway;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class ConfirmationIntentClassifier {

    private static final List<Pattern> EXPLICIT_CONFIRM_PATTERNS = List.of(
            Pattern.compile("^\\s*(yes|yes confirm|confirm|confirm it|go ahead|do it|proceed)\\s*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*(yes[,\\s]+)?confirm( the)? payment\\s*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*(yes[,\\s]+)?please proceed\\s*$", Pattern.CASE_INSENSITIVE)
    );

    public boolean isExplicitConfirmation(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = message.trim();

        return EXPLICIT_CONFIRM_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(normalized).matches());
    }
}