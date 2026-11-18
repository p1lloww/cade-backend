package br.ucsal.cade.shared.security.cors;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Origens do front-end que podem chamar a API pelo navegador (D09), de {@code cade.cors.*}.
 *
 * @param allowedOrigins origens exatas, como {@code https://cade.vercel.app}; no {@code CORS_ORIGINS}, separadas por
 *                       vírgula
 */
@Validated
@ConfigurationProperties("cade.cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {
}
