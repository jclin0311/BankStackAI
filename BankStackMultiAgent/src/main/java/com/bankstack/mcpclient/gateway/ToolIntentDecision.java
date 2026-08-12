package com.bankstack.mcpclient.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;

public record ToolIntentDecision(
        String toolName,
        double confidence,
        String reason
) {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static ToolIntentDecision from(String response) {
        try {
            String json = extractJson(response);
            return mapper.readValue(json, ToolIntentDecision.class);
        } catch (Exception e) {
            return new ToolIntentDecision(null, 0.0, "parse_failed");
        }
    }

    public boolean isConfident() {
        return confidence >= 0.75 && toolName != null && !toolName.isBlank();
    }

    private static String extractJson(String response) {
        if (response == null) {
            return "";
        }

        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return response;
    }
}
