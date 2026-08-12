package com.bankstack.mcp.security;

import com.account.dto.AccountOwnerResponse;
import com.bankstack.mcp.client.AccountServiceClient;
import com.commons.exception.MissingCustomerContextException;
import com.commons.exception.OwnershipViolationException;
import com.bankstack.mcp.safety.ToolAccessRole;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class OwnershipGuardService {

    private final AccountServiceClient accountServiceClient;

    public OwnershipGuardService(AccountServiceClient accountServiceClient) {
        this.accountServiceClient = accountServiceClient;
    }

    public void checkCustomerProfileAccess(CurrentActor actor, String requestedExternalId) {
        if (isElevated(actor.role())) {
            return;
        }

        requireCustomerExternalId(actor);

        if (!Objects.equals(actor.customerExternalId(), requestedExternalId)) {
            throw new OwnershipViolationException("You are not allowed to access another customer's profile.");
        }
    }

    public void checkAccountAccess(CurrentActor actor, UUID accountId) {
        if (isElevated(actor.role())) {
            return;
        }

        requireCustomerExternalId(actor);

        AccountOwnerResponse owner = accountServiceClient.getAccountOwner(accountId);
        String ownerCustomerId = owner.getCustomerId();

        if (!Objects.equals(actor.customerExternalId(), ownerCustomerId)) {
            throw new OwnershipViolationException("You are not allowed to access this account.");
        }
    }

    public void checkPaymentAccess(CurrentActor actor, UUID debtorAccountId) {
        checkAccountAccess(actor, debtorAccountId);
    }

    private boolean isElevated(ToolAccessRole role) {
        return role == ToolAccessRole.ADMIN || role == ToolAccessRole.INTERNAL_STAFF;
    }

    private void requireCustomerExternalId(CurrentActor actor) {
        if (actor.customerExternalId() == null || actor.customerExternalId().isBlank()) {
            throw new MissingCustomerContextException(
                    "Authenticated customer token does not contain a customer external id."
            );
        }
    }
}
