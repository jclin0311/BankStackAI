🛡️ commons-security

commons-security is a shared Spring Boot library that centralizes authentication and authorization configuration across all microservices in the Digital Bank platform.
It provides a consistent, opinionated security setup — so every service validates JWT tokens, enforces scopes, and participates in our Zero Trust architecture without duplicating code.


🚀 How to Use
1. Add dependency

In any microservice pom.xml:

<dependency>
  <groupId>com.digitalbank</groupId>
  <artifactId>commons-security</artifactId>
  <version>1.0.0</version>
</dependency>

2. Configure application.yml

Each microservice defines the issuer and audience:

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://dev-xxxxxx.us.auth0.com/
          audiences: https://mockbank/api


issuer-uri: points to your Auth0 tenant (used to fetch JWKS and validate tokens)

audiences: identifies the API resource this service represents

3. Secure your APIs

Controllers are secured by default. Use @PreAuthorize for fine-grained access:

@GetMapping("/accounts")
@PreAuthorize("hasAuthority('SCOPE_fdx:accounts.read')")
public List<AccountResponse> getAccounts() {
    return accountService.getAll();
}

