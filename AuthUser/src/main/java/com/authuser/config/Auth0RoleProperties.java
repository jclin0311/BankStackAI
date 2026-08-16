package com.authuser.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Maps the role names callers use ({@code ROLE_CUSTOMER}, {@code ROLE_ADMIN}) to the
 * Auth0 role ids they correspond to.
 *
 * <p>Role ids are tenant-specific, so provisioning takes a role <em>name</em> from the
 * caller and resolves it here rather than accepting a raw {@code rol_…} id over the
 * wire — a caller should not be able to grant an arbitrary role by guessing its id.</p>
 */
@Component
@ConfigurationProperties(prefix = "auth0.mgmt")
public class Auth0RoleProperties {

    private Map<String, String> roles = new LinkedHashMap<>();

    public Map<String, String> getRoles() {
        return roles;
    }

    public void setRoles(Map<String, String> roles) {
        this.roles = roles;
    }
}
