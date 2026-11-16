package br.ucsal.cade.access.api.event;

import java.time.Instant;
import java.util.Objects;

/**
 * Um código de login foi emitido e precisa ser enviado por e-mail ao dono (RF01). Publicado pelo módulo de acesso; o
 * módulo de avisos envia o e-mail de forma assíncrona (AVI-01).
 *
 * <p>É o único lugar onde o código puro existe: ele nunca é guardado e fica fora do {@link #toString()}, para não
 * vazar em logs.
 *
 * @param email     e-mail institucional normalizado
 * @param code      o código puro de seis dígitos
 * @param expiresAt quando o código deixa de valer, para mostrar no e-mail
 */
public record LoginCodeRequestedEvent(String email, String code, Instant expiresAt) {

    public LoginCodeRequestedEvent {
        Objects.requireNonNull(email);
        Objects.requireNonNull(code);
        Objects.requireNonNull(expiresAt);
    }

    @Override
    public String toString() {
        return "LoginCodeRequestedEvent[email=" + email + ", code=******, expiresAt=" + expiresAt + "]";
    }
}
