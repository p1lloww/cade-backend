package br.ucsal.cade.notifications.service.outbox.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Como a caixa de saída reenvia, de {@code cade.notifications.*}.
 *
 * @param maxAttempts quantas tentativas antes de abandonar o aviso (RNF08)
 * @param retryDelay  a espera depois da primeira falha; cresce a cada nova falha
 * @param batchSize   quantos avisos cada rodada do reenvio pega
 */
@Validated
@ConfigurationProperties("cade.notifications")
public record OutboxProperties(@Min(1) int maxAttempts, @NotNull Duration retryDelay, @Min(1) int batchSize) {
}
