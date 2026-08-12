package com.bankstack.mcpclient.gateway;

import com.bankstack.mcpclient.config.GatewayConversationProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConversationContextStore {

    private final ConcurrentHashMap<String, ConversationContext> store =
            new ConcurrentHashMap<>();

    private final GatewayConversationProperties properties;
    private final Clock clock = Clock.systemUTC();

    public ConversationContextStore(GatewayConversationProperties properties) {
        this.properties = properties;
    }

    public ConversationContext get(String sessionKey) {
        ConversationContext context = store.get(sessionKey);

        if (context == null) {
            return ConversationContext.empty();
        }

        if (isExpired(context)) {
            store.remove(sessionKey);
            return ConversationContext.empty();
        }

        return context;
    }

    public void saveAwaitingTool(String sessionKey,
                                 String tool,
                                 List<String> missingFields,
                                 Map<String, Object> newArguments) {

        ConversationContext current = get(sessionKey);

        Map<String, Object> mergedArguments =
                new HashMap<>(safeMap(current.awaitingArgumentData()));

        safeMap(newArguments).forEach((key, value) -> {
            if (!isBlank(value)) {
                mergedArguments.put(key, value);
            }
        });

        Map<String, Object> mergedEntities =
                new HashMap<>(safeMap(current.entities()));

        mergedEntities.putAll(mergedArguments);

        store.put(sessionKey, new ConversationContext(
                conversationId(sessionKey, current),
                Map.copyOf(mergedEntities),
                Instant.now(clock),

                preparedActionType(current),
                current.preparedAt(),
                current.lastPreparedSummary(),
                current.confirmationToken(),
                safeMap(current.preparedActionData()),

                blankToNull(tool),
                safeList(missingFields),
                Map.copyOf(mergedArguments),

                false,
                Map.of()
        ));
    }

    public void savePreparedAction(String sessionKey,
                                   PreparedActionType actionType,
                                   String summary,
                                   String confirmationToken,
                                   Map<String, Object> preparedData) {

        ConversationContext current = get(sessionKey);

        store.put(sessionKey, new ConversationContext(
                conversationId(sessionKey, current),
                safeMap(current.entities()),
                Instant.now(clock),

                actionType == null ? PreparedActionType.NONE : actionType,
                Instant.now(clock),
                summary,
                confirmationToken,
                safeMap(preparedData),

                null,
                List.of(),
                Map.of(),

                false,
                Map.of()
        ));
    }

    public void saveRecoveredSuggestion(String sessionKey,
                                        String tool,
                                        List<String> missingFields,
                                        Map<String, Object> awaitingArguments,
                                        Map<String, Object> recoveredArguments) {

        ConversationContext current = get(sessionKey);

        store.put(sessionKey, new ConversationContext(
                conversationId(sessionKey, current),
                safeMap(current.entities()),
                Instant.now(clock),

                preparedActionType(current),
                current.preparedAt(),
                current.lastPreparedSummary(),
                current.confirmationToken(),
                safeMap(current.preparedActionData()),

                blankToNull(tool),
                safeList(missingFields),
                safeMap(awaitingArguments),

                true,
                safeMap(recoveredArguments)
        ));
    }

    public void mergeRecoveredArguments(String sessionKey) {
        ConversationContext current = get(sessionKey);

        Map<String, Object> mergedArguments =
                new HashMap<>(safeMap(current.awaitingArgumentData()));

        safeMap(current.suggestedRecoveredArguments()).forEach((key, value) -> {
            if (!isBlank(value)) {
                mergedArguments.put(key, value);
            }
        });

        Map<String, Object> mergedEntities =
                new HashMap<>(safeMap(current.entities()));

        mergedEntities.putAll(mergedArguments);

        store.put(sessionKey, new ConversationContext(
                conversationId(sessionKey, current),
                Map.copyOf(mergedEntities),
                Instant.now(clock),

                preparedActionType(current),
                current.preparedAt(),
                current.lastPreparedSummary(),
                current.confirmationToken(),
                safeMap(current.preparedActionData()),

                current.awaitingTool(),
                safeList(current.missingFields()),
                Map.copyOf(mergedArguments),

                false,
                Map.of()
        ));
    }

    public void clearRecoveredSuggestion(String sessionKey) {
        ConversationContext current = get(sessionKey);

        store.put(sessionKey, new ConversationContext(
                current.conversationId(),
                safeMap(current.entities()),
                Instant.now(clock),

                preparedActionType(current),
                current.preparedAt(),
                current.lastPreparedSummary(),
                current.confirmationToken(),
                safeMap(current.preparedActionData()),

                current.awaitingTool(),
                safeList(current.missingFields()),
                safeMap(current.awaitingArgumentData()),

                false,
                Map.of()
        ));
    }

    public void clearAwaiting(String sessionKey) {
        ConversationContext current = get(sessionKey);

        store.put(sessionKey, new ConversationContext(
                current.conversationId(),
                safeMap(current.entities()),
                current.lastUpdatedAt(),

                preparedActionType(current),
                current.preparedAt(),
                current.lastPreparedSummary(),
                current.confirmationToken(),
                safeMap(current.preparedActionData()),

                null,
                List.of(),
                Map.of(),

                false,
                Map.of()
        ));
    }

    public void clear(String sessionKey) {
        store.remove(sessionKey);
    }

    private boolean isExpired(ConversationContext context) {

        Instant reference =
                context.lastUpdatedAt() != null
                        ? context.lastUpdatedAt()
                        : context.preparedAt();

        if (reference == null) {
            return false;
        }

        long ttl = properties.preparedActionTtlSeconds();

        if (ttl <= 0) {
            return false;
        }

        return Instant.now(clock).isAfter(reference.plusSeconds(ttl));
    }

    private String conversationId(String sessionKey,
                                  ConversationContext context) {

        if (context.conversationId() != null
                && !context.conversationId().isBlank()) {
            return context.conversationId();
        }

        return UUID.nameUUIDFromBytes(sessionKey.getBytes()).toString();
    }

    private static PreparedActionType preparedActionType(ConversationContext context) {
        return context.preparedActionType() == null ? PreparedActionType.NONE : context.preparedActionType();
    }

    private static Map<String, Object> safeMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(map);
    }

    private static List<String> safeList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return List.copyOf(list);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static boolean isBlank(Object value) {
        return value == null || value.toString().trim().isEmpty();
    }
}