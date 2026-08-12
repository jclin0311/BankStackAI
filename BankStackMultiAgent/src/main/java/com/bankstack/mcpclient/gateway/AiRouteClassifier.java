package com.bankstack.mcpclient.gateway;

import com.bankstack.mcpclient.prompt.AgentPromptLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AiRouteClassifier {

    private static final Logger log = LoggerFactory.getLogger(AiRouteClassifier.class);

    private final AgentPromptLoader promptLoader;
    private final LlmStructuredOutputService llmStructuredOutputService;

    public AiRouteClassifier(AgentPromptLoader promptLoader,
                             LlmStructuredOutputService llmStructuredOutputService) {
        this.promptLoader = promptLoader;
        this.llmStructuredOutputService = llmStructuredOutputService;
    }

    public RouteDecision classify(String message, ConversationContext context) {
        try {
            String systemPrompt = promptLoader.load("prompts/agent/route-classifier.system.txt");
            String contextInfo = buildContext(context);

            return llmStructuredOutputService.call(
                    systemPrompt,
                    "Message:\n" + (message == null ? "" : message) + "\n\nContext:\n" + contextInfo,
                    RouteDecision.class,
                    RouteDecision.fallback()
            );
        } catch (Exception ex) {
            log.warn("Route structured output failed. Falling back. message={}", message, ex);
            return RouteDecision.fallback();
        }
    }

    private String buildContext(ConversationContext context) {
        if (context == null) return "No prior context";

        if (context.isAwaitingInput()) {
            return "The system is waiting for missing input for tool: "
                    + context.awaitingTool()
                    + "; missing fields: "
                    + context.missingFields();
        }

        if (context.hasPreparedAction()) {
            return "User is in a prepared action flow: " + context.preparedActionType();
        }

        return "No active workflow context";
    }
}
