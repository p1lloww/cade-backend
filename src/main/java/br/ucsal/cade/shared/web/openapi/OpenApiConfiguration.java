package br.ucsal.cade.shared.web.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API documentation served by springdoc: the OpenAPI document at {@code /v3/api-docs} and the Swagger UI at
 * {@code /swagger-ui.html}. Every operation requires the access token unless it declares otherwise.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    /** Name of the security scheme that operations reference in {@code @SecurityRequirement}. */
    public static final String BEARER_AUTH = "bearer-jwt";

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cadê? API")
                        .version("v1")
                        .description("""
                                Back end of Cadê?, the UCSal lost and found system.

                                To call a protected route, get a token from `POST /auth/token`, click **Authorize** \
                                and paste it. Errors follow RFC 9457 (`application/problem+json`): `detail` holds \
                                the message to show to the user."""))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token from `POST /auth/token`, valid for 8 hours.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
