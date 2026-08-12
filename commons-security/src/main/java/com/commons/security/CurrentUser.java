package com.commons.security;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import org.springframework.http.HttpStatus;


import jakarta.servlet.http.HttpServletRequest;


@Component
public class CurrentUser {
	public Optional<String> customerId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Optional.empty();
        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Object val = jwt.getClaims().get("customer_id");
            if (val != null) return Optional.of(String.valueOf(val));
        }
        return Optional.empty();
    }

    public boolean hasScope(String scope) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SCOPE_" + scope));
    }
}