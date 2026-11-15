package br.ucsal.cade.access.api.event;

import java.time.Instant;
import java.util.Objects;

/**
 * A login code was issued and must be e-mailed to its owner (RF01). Published by the access module; the notifications
 * module sends the e-mail asynchronously (AVI-01).
 *
 * <p>This is the only place where the raw code exists: it is never stored and is hidden from {@link #toString()} so
 * it cannot leak into logs.
 *
 * @param email     normalized institutional e-mail
 * @param code      the raw six-digit code
 * @param expiresAt when the code stops being valid, to show in the e-mail
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
