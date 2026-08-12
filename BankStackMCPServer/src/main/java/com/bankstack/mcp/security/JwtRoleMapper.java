package com.bankstack.mcp.security;

import com.commons.exception.ToolAccessDeniedException;
import com.bankstack.mcp.safety.ToolAccessRole;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class JwtRoleMapper {

    private final ToolSecurityProperties properties;

    public JwtRoleMapper(ToolSecurityProperties properties) {
        this.properties = properties;
    }

    public ToolAccessRole mapRoles(Collection<String> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            throw new ToolAccessDeniedException("JWT does not contain any authorities.");
        }

        Set<String> normalizedAuthorities = new LinkedHashSet<>(authorities);

        if (matches(normalizedAuthorities, properties.adminRole(), properties.adminAuthorities())) {
            return ToolAccessRole.ADMIN;
        }


        if (matches(normalizedAuthorities, properties.customerRole(), properties.customerAuthorities())) {
            return ToolAccessRole.CUSTOMER;
        }

        throw new ToolAccessDeniedException("JWT authorities do not map to any supported tool access role.");
    }

    private boolean matches(Set<String> authorities, String primaryAuthority, Collection<String> additionalAuthorities) {
        if (primaryAuthority != null && authorities.contains(primaryAuthority)) {
            return true;
        }

        if (additionalAuthorities == null || additionalAuthorities.isEmpty()) {
            return false;
        }

        return additionalAuthorities.stream()
                .filter(value -> value != null && !value.isBlank())
                .anyMatch(authorities::contains);
    }
}
