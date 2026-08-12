package com.bankstack.mcp.confirmation;

import java.util.Optional;

public interface PendingToolActionStore {

    void save(PendingToolAction action);

    Optional<PendingToolAction> findByToken(String token);

    void delete(String token);
}