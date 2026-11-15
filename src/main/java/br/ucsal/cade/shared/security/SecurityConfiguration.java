package br.ucsal.cade.shared.security;

import br.ucsal.cade.shared.security.jwt.TokenClaims;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The back end as an OAuth 2.0 resource server (D09): every request outside the public routes must carry
 * {@code Authorization: Bearer <access token>}. There is no session and no cookie, so CSRF protection is not needed.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    /**
     * Routes open to anyone: getting a token, the health check, the API documentation, and {@code /error}, where
     * Spring forwards failed requests; without it, a 400 on a public route would turn into a 401.
     */
    private static final String[] PUBLIC_ROUTES = {
            "/auth/**", "/actuator/health", "/error", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(server -> server
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(roleConverter())));
        return http.build();
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
