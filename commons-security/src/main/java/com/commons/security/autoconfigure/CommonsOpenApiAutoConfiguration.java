package com.commons.security.autoconfigure;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Declares the JWT bearer scheme for every service that exposes Swagger UI.
 *
 * <p>All BankStack APIs are OAuth2 resource servers, so without a declared
 * security scheme Swagger UI has no "Authorize" button and every "Try it out"
 * returns 401. Applying the scheme globally means each operation sends
 * {@code Authorization: Bearer <token>} once a token is entered.</p>
 *
 * <p>Backs off entirely when springdoc is not on the classpath.</p>
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
public class CommonsOpenApiAutoConfiguration {

    private static final String SCHEME_NAME = "bearerAuth";

    /** Names the spec after the service, so published docs are distinguishable. */
    @Value("${spring.application.name:BankStack service}")
    private String applicationName;

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI bankStackOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName)
                        .version("v1")
                        .description("BankStackAI — " + applicationName))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Auth0 access token. Paste the raw JWT — Swagger adds the \"Bearer \" prefix.")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}
