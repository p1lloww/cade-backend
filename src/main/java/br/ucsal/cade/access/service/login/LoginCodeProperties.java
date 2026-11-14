package br.ucsal.cade.access.service.login;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Login code rules (D03), from {@code cade.login-code.*}. Checked on startup: a missing or invalid value stops the
 * application instead of failing on the first login.
 *
 * @param ttl            how long a code stays valid
 * @param maxAttempts    wrong attempts allowed per code
 * @param resendInterval minimum time between two codes for the same e-mail
 */
@Validated
@ConfigurationProperties("cade.login-code")
public record LoginCodeProperties(
        @NotNull Duration ttl,
        @Min(1) int maxAttempts,
        @NotNull Duration resendInterval) {
}
