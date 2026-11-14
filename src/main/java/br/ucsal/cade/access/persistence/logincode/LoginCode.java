package br.ucsal.cade.access.persistence.logincode;

import br.ucsal.cade.access.persistence.user.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * One-time login code sent to an institutional e-mail (RF01, D02, D03). Kept in Redis by {@link LoginCodeStore},
 * never in the database: it is useless once used or expired, and Redis deletes it on its own when it expires.
 *
 * @param email     normalized e-mail the code was sent to
 * @param codeHash  hash of the code; the raw code is never stored
 * @param createdAt when the code was issued
 * @param expiresAt when the code stops being valid
 * @param attempts  attempts already made with this code
 */
public record LoginCode(String email, String codeHash, Instant createdAt, Instant expiresAt, int attempts) {

    public LoginCode {
        email = User.normalize(email);
        Objects.requireNonNull(codeHash);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(expiresAt);
    }

    /**
     * A new code for the given e-mail, with no attempts yet.
     *
     * @param codeHash hash of the raw code
     * @param ttl      how long the code stays valid
     */
    public static LoginCode issue(String email, String codeHash, Instant now, Duration ttl) {
        return new LoginCode(email, codeHash, now, now.plus(ttl), 0);
    }

    /** Whether the code typed by the user is this one. */
    public boolean matches(String rawCode, PasswordEncoder encoder) {
        return rawCode != null && encoder.matches(rawCode, codeHash);
    }

    @Override
    public String toString() {
        return "LoginCode[email=" + email + ", createdAt=" + createdAt + ", expiresAt=" + expiresAt
                + ", attempts=" + attempts + "]";
    }
}
