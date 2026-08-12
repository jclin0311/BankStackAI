package com.commons.security.context;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * AuthenticatedCaller is an immutable view of the current authenticated principal,
 * built from the validated {@link Jwt} and the authorities Spring Security derived
 * from it (see {@link com.commons.security.JwtToAuthConverter}).
 *
 * <p>It exists so downstream services (MCP server tools, RAG retrieval, agents)
 * can reason about "who is calling" without touching Spring Security internals.</p>
 */
public class AuthenticatedCaller {

    private static final String SCOPE_PREFIX = "SCOPE_";
    private static final String ACTOR_ID_CLAIM = "actor_id";

    private final Jwt jwt;
    private final Set<String> authorities;

    public AuthenticatedCaller(Jwt jwt, Collection<String> authorities) {
        if (jwt == null) {
            throw new IllegalArgumentException("jwt must not be null");
        }
        this.jwt = jwt;
        this.authorities = authorities == null
                ? Set.of()
                : Set.copyOf(new LinkedHashSet<>(authorities));
    }

    /**
     * The JWT subject (Auth0 {@code sub} claim), e.g. {@code auth0|65f1...}.
     */
    public String subject() {
        return jwt.getSubject();
    }

    /**
     * Stable identifier for audit trails and pending-action ownership checks.
     * Uses the {@code actor_id} claim when the tenant provides one, otherwise
     * falls back to the JWT subject.
     */
    public String actorId() {
        String claim = jwt.getClaimAsString(ACTOR_ID_CLAIM);
        return (claim == null || claim.isBlank()) ? subject() : claim;
    }

    /**
     * The granted authorities exactly as Spring Security resolved them,
     * e.g. {@code SCOPE_fdx:accounts.read}.
     */
    public Collection<String> authorities() {
        return authorities;
    }

    /**
     * The raw OAuth2 scopes without the Spring {@code SCOPE_} prefix,
     * e.g. {@code fdx:accounts.read}.
     */
    public Set<String> scopes() {
        Set<String> scopes = new LinkedHashSet<>();
        for (String authority : authorities) {
            if (authority.startsWith(SCOPE_PREFIX)) {
                scopes.add(authority.substring(SCOPE_PREFIX.length()));
            }
        }
        return Set.copyOf(scopes);
    }

    /**
     * True if the caller holds at least one of the given authorities
     * (compared against the prefixed form, e.g. {@code SCOPE_rag:admin}).
     */
    public boolean hasAnyAuthority(Collection<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return false;
        }
        for (String candidate : candidates) {
            if (authorities.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads an arbitrary claim from the underlying JWT as a string,
     * or {@code null} when the claim is absent.
     */
    public String claimAsString(String claimName) {
        return claimName == null ? null : jwt.getClaimAsString(claimName);
    }
}
