package com.bankstack.mcp.confirmation;

import com.commons.exception.ConfirmationOwnershipException;
import com.commons.exception.ConfirmationTokenExpiredException;
import com.commons.exception.ConfirmationToolMismatchException;
import com.commons.exception.InvalidConfirmationTokenException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class ConfirmationService {

    private static final long DEFAULT_EXPIRY_MINUTES = 10;

    private final PendingToolActionStore pendingToolActionStore;
    private final ConfirmationTokenGenerator confirmationTokenGenerator;

    public ConfirmationService(PendingToolActionStore pendingToolActionStore,
                               ConfirmationTokenGenerator confirmationTokenGenerator) {
        this.pendingToolActionStore = pendingToolActionStore;
        this.confirmationTokenGenerator = confirmationTokenGenerator;
    }

    public PendingToolAction createPendingAction(String toolName,
                                                 String actorId,
                                                 Map<String, Object> payload) {
        String token = confirmationTokenGenerator.generate();
        OffsetDateTime now = OffsetDateTime.now();

        PendingToolAction action = new PendingToolAction(
                token,
                toolName,
                actorId,
                payload,
                now,
                now.plusMinutes(DEFAULT_EXPIRY_MINUTES)
        );

        pendingToolActionStore.save(action);
        return action;
    }

    public PendingToolAction consume(String token, String expectedToolName, String actorId) {
        PendingToolAction action = pendingToolActionStore.findByToken(token)
                .orElseThrow(() -> new InvalidConfirmationTokenException("Invalid confirmation token."));

        if (!action.toolName().equals(expectedToolName)) {
            throw new ConfirmationToolMismatchException(
                    "Confirmation token does not belong to tool '%s'.".formatted(expectedToolName)
            );
        }

        if (!action.actorId().equals(actorId)) {
            throw new ConfirmationOwnershipException(
                    "Confirmation token does not belong to the current actor."
            );
        }

        if (action.expiresAt().isBefore(OffsetDateTime.now())) {
            pendingToolActionStore.delete(token);
            throw new ConfirmationTokenExpiredException(
                    "Confirmation token has expired. Prepare the bill payment again."
            );
        }

        pendingToolActionStore.delete(token);
        return action;
    }
}
