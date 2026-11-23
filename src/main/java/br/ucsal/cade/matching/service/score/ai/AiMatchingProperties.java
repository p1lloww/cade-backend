package br.ucsal.cade.matching.service.score.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * A IA como critério da compatibilidade (RF25), de {@code cade.matching.ai.*}. Desligada por padrão.
 *
 * @param enabled   liga o critério; precisa de um bean {@link AiTextSimilarity}, que o projeto não traz
 * @param timeout   quanto esperar a IA por par; passou disso, o critério vale 0
 * @param maxPoints o máximo de pontos que a IA soma à nota (a soma total continua limitada a 100)
 */
@Validated
@ConfigurationProperties("cade.matching.ai")
public record AiMatchingProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("2s") @NotNull Duration timeout,
        @DefaultValue("15") @Min(0) @Max(100) int maxPoints) {
}
