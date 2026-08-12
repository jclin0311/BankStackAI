package com.payments.orch.security;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class FeignM2MOAuth2Config {

  // From application.yml: auth0.audience: https://mockbank/api
  @Value("${auth0.audience}")
  private String audience;

  /**
   * A dedicated AuthorizedClientManager for this Feign client only.
   * We name + qualify it to avoid conflicts when multiple managers exist.
   */
  @Bean("accountM2MAuthorizedClientManager")
  public OAuth2AuthorizedClientManager accountM2MAuthorizedClientManager(
      ClientRegistrationRepository registrations,
      OAuth2AuthorizedClientService clientService
  ) {
    // This converter builds the default form body for client_credentials token call
    var baseConverter = new OAuth2ClientCredentialsGrantRequestEntityConverter();

    // Customize token client to add Auth0's required "audience" parameter
    var tokenClient = new DefaultClientCredentialsTokenResponseClient();
    tokenClient.setRequestEntityConverter(grantRequest -> {
      RequestEntity<?> entity = baseConverter.convert(grantRequest);

      @SuppressWarnings("unchecked")
      MultiValueMap<String, String> form =
          new LinkedMultiValueMap<>((MultiValueMap<String, String>) entity.getBody());

      form.add("audience", audience);

      return new RequestEntity<>(
          form,
          entity.getHeaders(),
          entity.getMethod(),
          entity.getUrl()
      );
    });

    var provider = new ClientCredentialsOAuth2AuthorizedClientProvider();
    provider.setAccessTokenResponseClient(tokenClient);

    var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clientService);
    manager.setAuthorizedClientProvider(provider);

    return manager;
  }

  /**
   * Feign interceptor that fetches an M2M token via Spring OAuth2 client_credentials
   * and attaches it as Authorization header.
   */
  @Bean
  public RequestInterceptor m2mFeignInterceptor(
      @Qualifier("accountM2MAuthorizedClientManager") OAuth2AuthorizedClientManager manager
  ) {
    return template -> {
      OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
          .withClientRegistrationId("account-m2m") // must match your YAML registration id
          .principal("payment-orchestrator")       // any fixed principal is fine
          .build();

      OAuth2AuthorizedClient client = manager.authorize(authorizeRequest);
      if (client == null || client.getAccessToken() == null) {
        throw new IllegalStateException("Failed to obtain M2M access token for account-m2m");
      }

      template.header("Authorization", "Bearer " + client.getAccessToken().getTokenValue());
    };
  }
}
