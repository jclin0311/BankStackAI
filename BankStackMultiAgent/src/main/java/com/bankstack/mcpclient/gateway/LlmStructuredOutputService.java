package com.bankstack.mcpclient.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class LlmStructuredOutputService {

    private static final Logger log = LoggerFactory.getLogger(LlmStructuredOutputService.class);

    private final ChatClient extractorChatClient;
    private final JsonRepairService jsonRepairService;
    private final ObjectMapper mapper;

    public LlmStructuredOutputService(@Qualifier("extractorChatClient") ChatClient extractorChatClient,
                                      JsonRepairService jsonRepairService,
                                      ObjectMapper mapper) {
        this.extractorChatClient = extractorChatClient;
        this.jsonRepairService = jsonRepairService;
        this.mapper = mapper;
    }

    public <T> T call(String systemPrompt,
                      String userPrompt,
                      Class<T> targetType,
                      T fallback) {
        String raw = null;
        try {
            raw = extractorChatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt == null ? "" : userPrompt)
                    .call()
                    .content();

            return parse(raw, targetType);
        } catch (Exception ex) {
            /*
             * Do not run another full LLM retry here.
             * The first call already used the fast extractor model, and JsonRepairService already
             * performs deterministic repair. A second generation can double latency.
             * Downstream normalizers/recovery will fill obvious missing fields from the original text.
             */
            log.warn("Structured output failed after deterministic JSON repair. Returning fallback. targetType={}, raw={}",
                    targetType.getSimpleName(), safe(raw), ex);
            return fallback;
        }
    }

    private <T> T parse(String raw, Class<T> targetType) throws Exception {
        String repaired = jsonRepairService.repair(raw);
        return mapper.readValue(repaired, targetType);
    }

    private String safe(String value) {
        if (value == null) {
            return "<null>";
        }
        return value.length() <= 2000 ? value : value.substring(0, 2000) + "...<truncated>";
    }
}
