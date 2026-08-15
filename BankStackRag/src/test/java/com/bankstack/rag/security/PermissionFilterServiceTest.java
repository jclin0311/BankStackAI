package com.bankstack.rag.security;

import com.bankstack.rag.retrieve.HybridSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionFilterServiceTest {

    private final PermissionFilterService service = new PermissionFilterService();

    private static HybridSearchResult chunk(String id, Map<String, Object> metadata) {
        return new HybridSearchResult(id, "text of " + id, metadata, 0.9, 0.8, 0.85);
    }

    private static AccessContext user(String... scopes) {
        return AccessContext.user(Set.of(scopes));
    }

    @Test
    void adminSeesEveryChunkRegardlessOfSensitivity() {
        AccessContext admin = AccessContext.admin(Set.of("rag:admin"));

        assertTrue(service.isAllowed(chunk("c1", Map.of("sensitivity", "ADMIN")), admin));
        assertTrue(service.isAllowed(chunk("c2", Map.of(
                "sensitivity", "INTERNAL",
                "allowedScopes", "some:other.scope")), admin));
    }

    @Test
    void publicChunksAreVisibleWithoutScopes() {
        assertTrue(service.isAllowed(chunk("c1", Map.of("sensitivity", "PUBLIC")), user()));
    }

    @Test
    void internalChunksRequireAnOverlappingScope() {
        HybridSearchResult result = chunk("c1", Map.of(
                "sensitivity", "INTERNAL",
                "allowedScopes", "fdx:accounts.read, fdx:payments.read"));

        assertTrue(service.isAllowed(result, user("fdx:accounts.read")));
        assertFalse(service.isAllowed(result, user("fdx:transfers.write")));
        assertFalse(service.isAllowed(result, user()));
    }

    @Test
    void internalChunkWithoutAllowedScopesIsDeniedToNonAdmins() {
        HybridSearchResult result = chunk("c1", Map.of("sensitivity", "INTERNAL"));

        // Omitting allowedScopes at ingest must not turn INTERNAL into PUBLIC.
        assertFalse(service.isAllowed(result, user()));
        assertFalse(service.isAllowed(result, user("fdx:accounts.read")));
        assertTrue(service.isAllowed(result, AccessContext.admin(Set.of("rag:admin"))));
    }

    @Test
    void internalChunkWithBlankAllowedScopesIsDenied() {
        assertFalse(service.isAllowed(chunk("c1", Map.of(
                "sensitivity", "INTERNAL",
                "allowedScopes", "  ,  ")), user("fdx:accounts.read")));
    }

    @Test
    void adminSensitivityIsDeniedToNonAdmins() {
        HybridSearchResult result = chunk("c1", Map.of("sensitivity", "ADMIN"));

        assertFalse(service.isAllowed(result, user()));
        // Holding the raw scope is not enough — only RagAccessContextMapper grants admin.
        assertFalse(service.isAllowed(result, user("fdx:accounts.read")));
    }

    @Test
    void unrecognizedSensitivityFailsClosed() {
        assertFalse(service.isAllowed(chunk("c1", Map.of("sensitivity", "SECRET")), user()));
        assertFalse(service.isAllowed(chunk("c2", Map.of("sensitivity", "intrenal")), user()));
    }

    @Test
    void chunksWithoutSensitivityMetadataRemainVisible() {
        assertTrue(service.isAllowed(chunk("c1", Map.of()), user()));
        assertTrue(service.isAllowed(chunk("c2", null), user()));
        assertTrue(service.isAllowed(chunk("c3", Map.of("source", "policy.pdf")), user()));
        assertTrue(service.isAllowed(chunk("c4", Map.of("sensitivity", "  ")), user()));
    }

    @Test
    void filterKeepsOnlyAllowedResults() {
        List<HybridSearchResult> results = List.of(
                chunk("public", Map.of("sensitivity", "PUBLIC")),
                chunk("internal-ok", Map.of(
                        "sensitivity", "INTERNAL",
                        "allowedScopes", "fdx:accounts.read")),
                chunk("internal-denied", Map.of(
                        "sensitivity", "INTERNAL",
                        "allowedScopes", "admin:users.write")),
                chunk("admin-only", Map.of("sensitivity", "ADMIN")));

        List<HybridSearchResult> filtered = service.filter(results, user("fdx:accounts.read"));

        assertEquals(List.of("public", "internal-ok"),
                filtered.stream().map(HybridSearchResult::chunkId).toList());
    }
}
