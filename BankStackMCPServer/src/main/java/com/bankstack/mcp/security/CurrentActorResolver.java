package com.bankstack.mcp.security;

import com.bankstack.mcp.safety.ToolAccessRole;
import com.commons.security.context.AuthenticatedCaller;
import com.commons.security.context.AuthenticatedCallerProvider;
import org.springframework.stereotype.Component;

@Component
public class CurrentActorResolver {

    private final AuthenticatedCallerProvider callerProvider;
    private final JwtRoleMapper jwtRoleMapper;
    private final ToolSecurityProperties properties;

    public CurrentActorResolver(AuthenticatedCallerProvider callerProvider,
                                JwtRoleMapper jwtRoleMapper,
                                ToolSecurityProperties properties) {
        this.callerProvider = callerProvider;
        this.jwtRoleMapper = jwtRoleMapper;
        this.properties = properties;
    }

    public CurrentActor resolve() {
        AuthenticatedCaller caller = callerProvider.currentCaller();
        ToolAccessRole mappedRole = jwtRoleMapper.mapRoles(caller.authorities());

        return new CurrentActor(
                caller.actorId(),
                caller.subject(),
                caller.claimAsString(properties.customerExternalIdClaim()),
                mappedRole
        );
    }
}
