package br.ucsal.cade.shared.security.cors;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * CORS da API (D09): só as origens do front-end chamam a API pelo navegador. O Spring Security aplica esta
 * configuração antes de conferir o token, então o preflight ({@code OPTIONS}) é respondido sem token.
 *
 * <p>Não há credenciais: o token vai no header {@code Authorization}, nunca em cookie.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CorsProperties.class)
public class CrossOriginConfiguration {

    /** Header que o front manda para o ngrok não responder com a página de aviso (D08). */
    static final String NGROK_SKIP_WARNING = "ngrok-skip-browser-warning";

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins().stream().map(String::strip).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, NGROK_SKIP_WARNING));

        configuration.setExposedHeaders(List.of(HttpHeaders.RETRY_AFTER));
        configuration.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
