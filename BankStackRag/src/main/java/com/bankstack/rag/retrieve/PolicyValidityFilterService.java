package com.bankstack.rag.retrieve;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Filters retrieved chunks by policy effective dates.
 *
 * A chunk is considered currently valid when:
 *
 * effectiveFrom is missing OR effectiveFrom <= now
 * AND
 * effectiveTo is missing OR effectiveTo >= now
 *
 * This prevents expired or future-dated policies from becoming evidence.
 */
@Service
public class PolicyValidityFilterService {

    private final Clock clock;

    public PolicyValidityFilterService() {
        this(Clock.systemUTC());
    }

    PolicyValidityFilterService(Clock clock) {
        this.clock = clock;
    }

    public List<HybridSearchResult> filterCurrent(List<HybridSearchResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        Instant now = Instant.now(clock);

        return results.stream()
                .filter(result -> isCurrentlyEffective(result.metadata(), now))
                .toList();
    }

    boolean isCurrentlyEffective(Map<String, Object> metadata, Instant now) {
        if (metadata == null || metadata.isEmpty()) {
            return true;
        }

        Instant effectiveFrom = parseInstant(metadata.get("effectiveFrom"));
        Instant effectiveTo = parseInstant(metadata.get("effectiveTo"));

        boolean started = effectiveFrom == null || !effectiveFrom.isAfter(now);
        boolean notExpired = effectiveTo == null || !effectiveTo.isBefore(now);

        return started && notExpired;
    }

    private Instant parseInstant(Object value) {
        if (value == null) {
            return null;
        }

        String raw = value.toString().trim();
        if (raw.isBlank() || raw.equalsIgnoreCase("null")) {
            return null;
        }

        try {
            return Instant.parse(raw);
        } catch (Exception ignored) {
            /*
             * If a date is malformed, fail closed.
             * A policy with broken validity metadata should not become evidence.
             */
            return Instant.MAX;
        }
    }
}
