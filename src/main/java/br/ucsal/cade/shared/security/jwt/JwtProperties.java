package br.ucsal.cade.shared.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuração do token de acesso (D02, D09), de {@code cade.jwt.*}.
 *
 * @param secret chave que assina e confere os tokens com HMAC-SHA256 ({@code JWT_SECRET}); o HS256 precisa de pelo
 *               menos 256 bits, ou seja, pelo menos 32 caracteres
 * @param ttl    por quanto tempo um token vale
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
