package com.commons.security.autoconfigure;

import com.commons.security.JwtToAuthConverter;
import com.commons.security.context.AuthenticatedCallerProvider;
import com.commons.security.context.UserSessionKeyResolver;
import com.commons.security.token.SecurityTokenProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for commons-security.
 *
 * <p>Services depend on this library from outside the {@code com.commons}
 * package, so component scanning never sees these classes. Registering them
 * here (via META-INF/spring/...AutoConfiguration.imports) makes the beans
 * available to every consuming Spring Boot application automatically.</p>
 *
 * <p>{@link com.commons.security.DefaultSecurityConfig} is intentionally NOT
 * auto-registered: services define their own {@code SecurityFilterChain} and
 * auto-activating a second chain would conflict. Services that want the
 * default chain can {@code @Import} it explicitly.</p>
 */
@AutoConfiguration
public class CommonsSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtToAuthConverter jwtToAuthConverter() {
        return new JwtToAuthConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticatedCallerProvider authenticatedCallerProvider() {
        return new AuthenticatedCallerProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public UserSessionKeyResolver userSessionKeyResolver(AuthenticatedCallerProvider callerProvider) {
        return new UserSessionKeyResolver(callerProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityTokenProvider securityTokenProvider() {
        return new SecurityTokenProvider();
    }
}
