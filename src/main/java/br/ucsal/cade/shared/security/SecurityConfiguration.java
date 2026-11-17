package br.ucsal.cade.shared.security;

import br.ucsal.cade.shared.security.jwt.TokenClaims;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * O back-end como servidor de recursos OAuth 2.0 (D09): toda requisição fora das rotas públicas precisa levar
 * {@code Authorization: Bearer <token de acesso>}. Não há sessão nem cookie, então a proteção contra CSRF não é
 * necessária.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    /**
     * Rotas abertas a qualquer um: obter o token, a verificação de saúde, a documentação da API e o {@code /error},
     * para onde o Spring encaminha as requisições que falham; sem ele, um 400 numa rota pública viraria 401.
     *
     * <p>Cada rota é listada sozinha, nunca com curinga como {@code /auth/**}: uma rota criada depois no mesmo caminho
     * precisa exigir token, a menos que alguém a libere aqui de propósito.
     */
    private static final String[] PUBLIC_ROUTES = {
            "/auth/login-code", "/auth/token", "/actuator/health", "/error",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver)
            throws Exception {
        AuthenticationEntryPoint unauthorized = problemDetailEntryPoint(resolver);
        AccessDeniedHandler forbidden = problemDetailAccessDeniedHandler(resolver);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden))
                .oauth2ResourceServer(server -> server
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(roleConverter())));
        return http.build();
    }

    /**
     * Responde 401 a uma requisição sem token válido. O entry point padrão do bearer define o status e o cabeçalho
     * {@code WWW-Authenticate}; depois a exceção vai para o {@code ApiExceptionHandler}, então o corpo é um problem
     * detail como todo outro erro (D17).
     */
    private static AuthenticationEntryPoint problemDetailEntryPoint(HandlerExceptionResolver resolver) {
        BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();
        return (request, response, exception) -> {
            bearer.commence(request, response, exception);
            resolver.resolveException(request, response, null, exception);
        };
    }

    /** O mesmo que o {@link #problemDetailEntryPoint}, para um token válido cujo perfil não abre a rota (403). */
    private static AccessDeniedHandler problemDetailAccessDeniedHandler(HandlerExceptionResolver resolver) {
        BearerTokenAccessDeniedHandler bearer = new BearerTokenAccessDeniedHandler();
        return (request, response, exception) -> {
            bearer.handle(request, response, exception);
            resolver.resolveException(request, response, null, exception);
        };
    }

    /**
     * Transforma a claim {@code role} numa authority do Spring ({@code STUDENT} vira {@code ROLE_STUDENT}), da qual a
     * autorização por perfil depende. O id da conta ({@code sub}) vira o nome da autenticação.
     */
    private static JwtAuthenticationConverter roleConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(TokenClaims.ROLE);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
