package com.bankstack.rag.security;

import java.util.Set;

/**
 * AccessContext represents the caller's retrieval permissions.
 *
 * Why this class exists:
 * Retrieval safety depends on WHO is asking.
 *
 * In compliance-grade RAG, we do not decide access only from the query text.
 * We decide access from:
 * - caller scopes
 * - caller role characteristics (e.g. admin or not)
 *
 * This object is intentionally small for now.
 *
 * Today it contains:
 * - grantedScopes
 * - admin flag
 *
 * Later it can evolve to include:
 * - customerId / ownership
 * - tenantId
 * - country
 * - product entitlements
 * - internal employee role
 */
public record AccessContext(
        Set<String> grantedScopes,
        boolean admin
) {

    /**
     * Convenience factory for a non-admin user.
     */
    public static AccessContext user(Set<String> scopes) {
        return new AccessContext(scopes, false);
    }

    /**
     * Convenience factory for an admin user.
     */
    public static AccessContext admin(Set<String> scopes) {
        return new AccessContext(scopes, true);
    }
}