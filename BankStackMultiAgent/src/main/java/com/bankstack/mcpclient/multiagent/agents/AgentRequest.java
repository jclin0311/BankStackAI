package com.bankstack.mcpclient.multiagent.agents;

import com.bankstack.mcpclient.gateway.ConversationContext;
import com.bankstack.mcpclient.multiagent.router.RoutedTask;

public record AgentRequest(
        String sessionKey,
        String message,
        RoutedTask task,
        ConversationContext conversationContext,
        boolean explicitConfirmation
) {}
