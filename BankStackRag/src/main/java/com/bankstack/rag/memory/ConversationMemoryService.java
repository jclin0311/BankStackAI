package com.bankstack.rag.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConversationMemoryService {

    private final ChatMemory chatMemory;

    public ConversationMemoryService(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    public String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return conversationId;
    }

    public void remember(String conversationId, String userQuery, String assistantAnswer) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }

        chatMemory.add(conversationId, new UserMessage(userQuery));
        chatMemory.add(conversationId, new AssistantMessage(assistantAnswer));
    }

    public String renderForPrompt(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "";
        }

        return chatMemory.get(conversationId).stream()
                .map(m -> m.getMessageType() + ": " + m.getText())
                .collect(Collectors.joining("\n"));
    }
}