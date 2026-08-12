package com.commons.security.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * AuthenticatedCallerProvider resolves the {@link AuthenticatedCaller} for the
 * current request from Spring Security's {@link SecurityContextHolder}.
 *
 * <p>It requires the request to have been authenticated as an OAuth2 resource
 * server JWT. Calling it outside an authenticated request (e.g. during
 * application startup) throws, so callers that may run in that window must
 * handle the exception themselves.</p>
 */
public class AuthenticatedCallerProvider {

    public AuthenticatedCaller currentCaller() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalStateException(
                    "No authenticated JWT caller is present in the security context.");
        }

        Set<String> authorities = new LinkedHashSet<>();
        for (GrantedAuthority authority : jwtAuthentication.getAuthorities()) {
            authorities.add(authority.getAuthority());
        }

        return new AuthenticatedCaller(jwtAuthentication.getToken(), authorities);
    }
}
