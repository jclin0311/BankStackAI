package com.bankstack.rag.prompt;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PromptRenderer {

    public String render(String template, Map<String, String> vars) {
        String rendered = template;

        for (Map.Entry<String, String> entry : vars.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() == null ? "" : entry.getValue();
            rendered = rendered.replace(placeholder, value);
        }

        return rendered;
    }
}