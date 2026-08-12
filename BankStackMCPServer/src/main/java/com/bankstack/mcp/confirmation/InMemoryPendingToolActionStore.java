package com.bankstack.mcp.confirmation;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Development/demo implementation.
 *
 * For production, replace this with a durable TTL-backed store such as Redis or a database table
 * so confirmation tokens survive restarts and expire consistently across multiple replicas.
 */
@Component
public class InMemoryPendingToolActionStore implements PendingToolActionStore {

    private final Map<String, PendingToolAction> store = new ConcurrentHashMap<>();

    @Override
    public void save(PendingToolAction action) {
        store.put(action.confirmationToken(), action);
    }

    @Override
    public Optional<PendingToolAction> findByToken(String token) {
        return Optional.ofNullable(store.get(token));
    }

    @Override
    public void delete(String token) {
        store.remove(token);
    }
}