package com.bankstack.rag.security;

import com.commons.security.context.AuthenticatedCaller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RagAccessContextMapper {

    private final Set<String> adminAuthorities;

    public RagAccessContextMapper(
            @Value("${bankstack.rag.security.admin-authorities:ROLE_ADMIN,SCOPE_rag:admin}")
            String configuredAdminAuthorities) {
        this.adminAuthorities = Arrays.stream(configuredAdminAuthorities.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public AccessContext from(AuthenticatedCaller caller) {
        boolean admin = caller.hasAnyAuthority(adminAuthorities);
        return admin
                ? AccessContext.admin(caller.scopes())
                : AccessContext.user(caller.scopes());
    }
}
