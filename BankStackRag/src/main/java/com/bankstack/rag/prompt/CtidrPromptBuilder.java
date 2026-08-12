package com.bankstack.rag.prompt;

import com.bankstack.rag.assemble.AssembledContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CtidrPromptBuilder {

    private final RagPromptTemplateLoader templateLoader;
    private final PromptRenderer renderer;

    public CtidrPromptBuilder(RagPromptTemplateLoader templateLoader,
                              PromptRenderer renderer) {
        this.templateLoader = templateLoader;
        this.renderer = renderer;
    }

    public CtidrPrompt build(PromptTask task, AssembledContext context) {

        String templateName = switch (task.type()) {
            case REFUSAL -> "refusal.system.txt";
            case POLICY_LOOKUP -> "policy.system.txt";
        };

        String systemTemplate = templateLoader.load(templateName);

        Map<String, String> vars = Map.of(
                "chunks", context == null || context.renderedContext() == null
                        ? ""
                        : context.renderedContext().trim()
        );

        String systemPrompt = renderer.render(systemTemplate, vars);

        String userPrompt = """
                QUESTION:
                %s
                """.formatted(task.userQuery());

        return new CtidrPrompt(systemPrompt, "", userPrompt);
    }
}