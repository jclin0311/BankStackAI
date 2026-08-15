package com.bankstack.mcp.security;

import com.commons.security.JwtToAuthConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(ToolSecurityProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtToAuthConverter jwtToAuthConverter) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // The MCP client performs its initialize/discovery handshake from a
                        // transport worker thread that has no user SecurityContext, so the
                        // handshake arrives anonymously (see McpClientConfig in the agent).
                        // Tool calls still relay the user's bearer token, and every tool
                        // resolves the caller via CurrentActorResolver, which throws when no
                        // JWT is present — so authorization is enforced per tool, not here.
                        .requestMatchers("/mcp", "/mcp/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtToAuthConverter)));

        return http.build();
    }
}
