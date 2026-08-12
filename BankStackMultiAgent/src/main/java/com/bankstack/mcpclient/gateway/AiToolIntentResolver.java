package com.bankstack.mcpclient.gateway;

import com.bankstack.mcpclient.prompt.AgentPromptLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AiToolIntentResolver {

    private static final Logger log = LoggerFactory.getLogger(AiToolIntentResolver.class);

    private final AgentPromptLoader loader;
    private final LlmStructuredOutputService llmStructuredOutputService;

    public AiToolIntentResolver(AgentPromptLoader loader,
                                LlmStructuredOutputService llmStructuredOutputService) {
        this.loader = loader;
        this.llmStructuredOutputService = llmStructuredOutputService;
    }

    public ToolIntentDecision resolve(String message,
                                      boolean explicitConfirmation,
                                      ConversationContext context) {
        try {
            String systemPrompt = loader.load("prompts/agent/intent-classifier.system.txt");

            String userPrompt = """
                    User Message:
                    %s

                    Explicit Confirmation:
                    %s

                    Conversation Context:
                    %s
                    """.formatted(
                    message == null ? "" : message,
                    explicitConfirmation,
                    context == null ? "N/A" : context.toString()
            );

            return llmStructuredOutputService.call(
                    systemPrompt,
                    userPrompt,
                    ToolIntentDecision.class,
                    new ToolIntentDecision(null, 0.0, "parse_failed")
            );
        } catch (Exception ex) {
            log.warn("Tool intent structured output failed. message={}", message, ex);
            return new ToolIntentDecision(null, 0.0, "parse_failed");
        }
    }
}
