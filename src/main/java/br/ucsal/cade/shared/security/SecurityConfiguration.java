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
 * The back end as an OAuth 2.0 resource server (D09): every request outside the public routes must carry
 * {@code Authorization: Bearer <access token>}. There is no session and no cookie, so CSRF protection is not needed.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    /**
     * Routes open to anyone: getting a token, the health check, the API documentation, and {@code /error}, where
     * Spring forwards failed requests; without it, a 400 on a public route would turn into a 401.
     *
     * <p>Each route is listed on its own, never with a wildcard such as {@code /auth/**}: a route added later under
     * the same path must ask for a token unless someone opens it on purpose here.
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
     * Answers a request without a valid token with 401. The standard bearer entry point sets the status and the
     * {@code WWW-Authenticate} header; then the exception goes to the {@code ApiExceptionHandler}, so the body is a
     * problem detail like every other error (D17).
     */
    private static AuthenticationEntryPoint problemDetailEntryPoint(HandlerExceptionResolver resolver) {
        BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();
        return (request, response, exception) -> {
            bearer.commence(request, response, exception);
            resolver.resolveException(request, response, null, exception);
        };
    }

    /** Same as {@link #problemDetailEntryPoint}, for a valid token whose role cannot open the route (403). */
    private static AccessDeniedHandler problemDetailAccessDeniedHandler(HandlerExceptionResolver resolver) {
        BearerTokenAccessDeniedHandler bearer = new BearerTokenAccessDeniedHandler();
        return (request, response, exception) -> {
            bearer.handle(request, response, exception);
            resolver.resolveException(request, response, null, exception);
        };
    }

    /**
     * Turns the {@code role} claim into a Spring authority ({@code STUDENT} becomes {@code ROLE_STUDENT}), which
     * role-based authorization relies on. The account id ({@code sub}) becomes the authentication name.
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
