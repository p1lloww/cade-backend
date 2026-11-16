package br.ucsal.cade.access.service.login;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Regras do código de login (D03, D18), de {@code cade.login-code.*}. Conferidas na subida: um valor ausente ou
 * inválido impede a aplicação de subir, em vez de falhar no primeiro login.
 *
 * @param ttl                   por quanto tempo um código vale
 * @param maxAttempts           tentativas erradas permitidas por código
 * @param resendInterval        tempo mínimo entre dois códigos para o mesmo e-mail
 * @param blockAfterLockedCodes quantos códigos um e-mail pode travar, esgotando as tentativas, antes de ser bloqueado
 * @param blockDuration         por quanto tempo um e-mail bloqueado não pode pedir código
 */
@Validated
@ConfigurationProperties("cade.login-code")
public record LoginCodeProperties(
        @NotNull Duration ttl,
        @Min(1) int maxAttempts,
        @NotNull Duration resendInterval,
        @Min(1) int blockAfterLockedCodes,
        @NotNull Duration blockDuration) {

    /** Tentativas erradas, somando todos os códigos, que bloqueiam o e-mail. */
    public int maxFailures() {
        return maxAttempts * blockAfterLockedCodes;
    }
}
