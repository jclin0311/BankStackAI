package com.bankstack.mcpclient.gateway;

import com.bankstack.mcpclient.prompt.AgentPromptLoader;
import org.springframework.stereotype.Component;

@Component
public class RefusalResponseFactory {

    private final AgentPromptLoader loader;

    public RefusalResponseFactory(AgentPromptLoader loader) {
        this.loader = loader;
    }

    public String create(String message) {
        return loader.load("prompts/agent/refusal.txt");
    }
}