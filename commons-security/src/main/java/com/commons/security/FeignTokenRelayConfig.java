package com.commons.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

/**
 * FeignTokenRelayConfig ensures that when one microservice calls another
 * using a Feign client, the original user's JWT access token is automatically
 * forwarded in the outgoing HTTP request.
 *
 * <p>This is essential for implementing <b>token relay</b> — where downstream services
 * can continue to authorize requests based on the same user identity and permissions.</p>
 *
 * <p>Without this, the second service would never know "who" the user is, because the token
 * would not be propagated across service boundaries.</p>
 */
@Configuration
public class FeignTokenRelayConfig {

  // The expected audience (API identifier) that must be present in the token
  @Value("${auth0.audience}")
  private String expectedAudience;

  /**
   * A Feign RequestInterceptor bean that automatically attaches the user's Bearer token
   * to outgoing HTTP requests if the current Authentication is a JwtAuthenticationToken.
   *
   * <p>This allows downstream microservices to receive and validate the same access token
   * that the user originally presented — enabling secure, end-to-end identity propagation.</p>
   */
  @Bean
  public RequestInterceptor relayUserJwt() {
    return new RequestInterceptor() {
      @Override
      public void apply(RequestTemplate tpl) {
        // Get the current authentication object from the security context
        Authentication a = SecurityContextHolder.getContext().getAuthentication();

        // Proceed only if the current user is authenticated with a JWT
        if (a instanceof JwtAuthenticationToken) {
          Jwt jwt = ((JwtAuthenticationToken) a).getToken();

          // Extract the 'aud' (audience) claim from the JWT
          Object aud = jwt.getClaims().get("aud");

          // Relay the token only if the expected audience is present
          // This ensures we don't forward tokens meant for other APIs
          if (aud instanceof List && ((List<?>) aud).contains(expectedAudience)) {
            // Add the Authorization header with the Bearer token to the outgoing Feign request
            tpl.header("Authorization", "Bearer " + jwt.getTokenValue());
          }
        }
      }
    };
  }
}
