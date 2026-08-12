package com.bankstack.mcpclient.guardrails;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResponseRedactionService {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b"
    );

    private static final Pattern LONG_ACCOUNT_NUMBER_PATTERN = Pattern.compile(
            "\\b(\\d{4})(\\d{4,12})(\\d{4})\\b"
    );

    private static final Pattern CONFIRMATION_TOKEN_PATTERN = Pattern.compile(
            "(?i)(confirmation\\s*(token|code)\\s*[:=]\\s*)([A-Za-z0-9._-]{6,})"
    );

    public String redact(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }

        String value = response;
        value = CONFIRMATION_TOKEN_PATTERN.matcher(value).replaceAll("$1[redacted-token]");
        value = UUID_PATTERN.matcher(value).replaceAll("[redacted-id]");
        value = maskLongAccountNumbers(value);
        return value;
    }

    private String maskLongAccountNumbers(String value) {
        Matcher matcher = LONG_ACCOUNT_NUMBER_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String replacement = matcher.group(1)
                    + "******"
                    + matcher.group(3);
            matcher.appendReplacement(buffer, replacement);
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
