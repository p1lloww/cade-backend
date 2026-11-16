package br.ucsal.cade.shared.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Access token settings (D02, D09), from {@code cade.jwt.*}.
 *
 * @param secret key that signs and verifies tokens with HMAC-SHA256 ({@code JWT_SECRET}); HS256 needs at least
 *               256 bits, so at least 32 characters
 * @param ttl    how long a token stays valid
 */
@Validated
@ConfigurationProperties("cade.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @NotNull Duration ttl) {

    @Override
    public String toString() {
        return "JwtProperties[secret=******, ttl=" + ttl + "]";
    }
}
