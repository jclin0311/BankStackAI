package com.bankstack.mcpclient.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatConfig {

    @Bean
    @Primary
    ChatClient reasoningChatClient(ChatClient.Builder builder,
                                   @Value("${bankstack.ai.reasoning.model:llama3.2:latest}") String model) {
        return builder
                .defaultOptions(OllamaChatOptions.builder()
                        .model(model)
                        .temperature(0.2)
                        .topP(0.8)
                        .numPredict(300)
                        .build())
                .build();
    }

    @Bean
    @Qualifier("extractorChatClient")
    ChatClient extractorChatClient(ChatClient.Builder builder,
                                   @Value("${bankstack.ai.extractor.model:qwen2.5:1.5b}") String model) {
        return builder
                .defaultOptions(OllamaChatOptions.builder()
                        .model(model)
                        .temperature(0.0)
                        .topP(0.1)
                        .numPredict(120)
                        .numCtx(1024)
                        .build())
                .build();
    }
}
