package com.bankstack.mcpclient.gateway;

import org.springframework.stereotype.Component;

@Component
public class ConfirmationGuardService {

    public void validateConfirmation(boolean explicitConfirmation, ConversationContext context) {
        if (!explicitConfirmation) {
            return;
        }

        if (context == null || !context.hasPreparedAction()) {
            throw new IllegalStateException(
                    "No prepared action exists to confirm. Please initiate the payment first."
            );
        }
    }
}