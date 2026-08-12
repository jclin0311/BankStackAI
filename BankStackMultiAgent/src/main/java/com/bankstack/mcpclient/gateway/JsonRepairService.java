package com.bankstack.mcpclient.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonRepairService {

    private final ObjectMapper mapper = new ObjectMapper();

    public String repair(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }

        String value = raw.trim();

        value = removeMarkdownFences(value);
        value = extractJsonObject(value);

        // first sanitation pass
        value = sanitizeMalformedJson(value);
        value = removeTrailingCommas(value);

        if (isValidJson(value)) {
            return value;
        }

        value = autoCloseJsonObject(value);

        // second sanitation pass after auto close
        value = sanitizeMalformedJson(value);
        value = removeTrailingCommas(value);

        return value;
    }
    private String removeMarkdownFences(String value) {
        return value
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();
    }

    private String extractJsonObject(String value) {
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return value.substring(start, end + 1).trim();
        }

        if (start >= 0) {
            return value.substring(start).trim();
        }

        return value;
    }

    private String autoCloseJsonObject(String value) {
        long openBraces = value.chars().filter(ch -> ch == '{').count();
        long closeBraces = value.chars().filter(ch -> ch == '}').count();

        String repaired = value;
        while (closeBraces < openBraces) {
            repaired += "}";
            closeBraces++;
        }

        return repaired;
    }

    private String removeTrailingCommas(String value) {
        return value.replaceAll(",\\s*([}\\]])", "$1");
    }

    private boolean isValidJson(String value) {
        try {
            mapper.readTree(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    
    
    private String sanitizeMalformedJson(String json) {
        if (json == null) {
            return "{}";
        }

        String value = json;

        for (int i = 0; i < 5; i++) {
            value = value
                    .replaceAll(",\\s*,", ",")
                    .replaceAll("\\{\\s*,", "{")
                    .replaceAll(",\\s*}", "}")
                    .replaceAll(",\\s*]", "]");
        }

        return value.trim();
    }
}
