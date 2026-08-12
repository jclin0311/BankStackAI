package com.commons.security.token;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * SecurityTokenProvider exposes the raw bearer token of the current request so
 * it can be relayed to downstream services (Feign clients, the MCP server).
 *
 * <p>The returned value is the bare JWT string without the {@code "Bearer "}
 * prefix; callers add the prefix when building the Authorization header.</p>
 */
public class SecurityTokenProvider {

    /**
     * @return the raw JWT of the current authenticated request
     * @throws IllegalStateException when no authenticated JWT request exists,
     *         e.g. during startup discovery calls made outside a user request
     */
    public String currentBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalStateException(
                    "No authenticated JWT is present in the security context.");
        }

        return jwtAuthentication.getToken().getTokenValue();
    }
}
