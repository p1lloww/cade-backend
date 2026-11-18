package br.ucsal.cade.shared.web.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação da API servida pelo springdoc: o documento OpenAPI em {@code /v3/api-docs} e o Swagger UI em
 * {@code /swagger-ui.html}. Toda operação exige o token de acesso, a menos que declare o contrário.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    /** Nome do esquema de segurança que as operações citam no {@code @SecurityRequirement}. */
    public static final String BEARER_AUTH = "bearer-jwt";

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cadê? API")
                        .version("v1")
                        .description("""
                                Back-end do Cadê?, o sistema de achados e perdidos da UCSal.

                                Para chamar uma rota protegida, obtenha um token em `POST /auth/token`, clique em \
                                **Authorize** e cole o token. Os erros seguem a RFC 9457 \
                                (`application/problem+json`): `detail` traz a mensagem a mostrar ao usuário."""))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token de acesso de `POST /auth/token`, válido por 8 horas.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
