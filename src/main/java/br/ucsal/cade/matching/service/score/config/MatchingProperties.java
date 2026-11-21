package br.ucsal.cade.matching.service.score.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Pesos e faixas da compatibilidade (RN07, RF21), de {@code cade.matching.*}. Conferidos na subida: um valor ausente
 * ou inválido impede a aplicação de subir.
 */
@Validated
@ConfigurationProperties("cade.matching")
public record MatchingProperties(
        @NotNull @Valid Date date,
        @NotNull @Valid Location location,
        @NotNull @Valid Words words,
        @NotNull @Valid Bands bands) {

    /**
     * @param recentDays       até quantos dias depois da perda o objeto vale {@code recentPoints}
     * @param closeDays        até quantos dias depois da perda o objeto vale {@code closePoints}
     * @param toleranceDays    quantos dias antes da perda o objeto ainda pode ter sido achado
     */
    public record Date(@Min(0) int recentDays, @Min(0) int recentPoints, @Min(0) int closeDays,
                       @Min(0) int closePoints, @Min(0) int toleranceDays) {
    }

    public record Location(@Min(0) int sameRoomPoints, @Min(0) int sameBlockPoints) {
    }

    /**
     * @param minLength palavras mais curtas não contam
     */
    public record Words(@Min(0) int pointsEach, @Min(0) int maxCounted, @Min(1) int minLength) {
    }

    /**
     * @param high   nota mínima da faixa alta
     * @param medium nota mínima da faixa média; abaixo dela, baixa
     */
    public record Bands(@Min(0) int high, @Min(0) int medium) {
    }
}
