package br.ucsal.cade.access.persistence.logincode;

import br.ucsal.cade.access.persistence.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * One-time login code sent to an institutional e-mail (RF01, D02, D03), in the {@code login_codes} table (D18).
 *
 * <p>There is at most one row per e-mail: a new code overwrites the previous one, so the table never grows past the
 * number of people who ever asked for a code and needs no cleanup. An expired row is simply ignored until the next
 * code overwrites it. A successful login deletes the row.
 *
 * <p>The row also keeps, across codes, the wrong attempts of the e-mail and the end of a block, so asking for a new
 * code does not reset the protection against guessing (D18). Changes are made by the atomic statements of
 * {@link LoginCodeStore}, never by saving this entity.
 */
@Entity
@Table(name = "login_codes")
public class LoginCode {

    /** Normalized e-mail the code was sent to. */
    @Id
    @Column(name = "email")
    private String email;

    /** Hash of the code; the raw code is never stored. */
    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Attempts made with the current code, right or wrong. */
    @Column(name = "attempts", nullable = false)
    private int attempts;

    /** Wrong attempts of the e-mail since the last block or login, adding up across codes. */
    @Column(name = "failures", nullable = false)
    private int failures;

    /** Until when the e-mail cannot ask for a code; {@code null} when it is not blocked. */
    @Column(name = "blocked_until")
    private Instant blockedUntil;

    protected LoginCode() {
    }

    LoginCode(String email, String codeHash, Instant createdAt, Instant expiresAt, int attempts, int failures,
              Instant blockedUntil) {
        this.email = User.normalize(email);
        this.codeHash = Objects.requireNonNull(codeHash);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.attempts = attempts;
        this.failures = failures;
        this.blockedUntil = blockedUntil;
    }

    /** A new code for the given e-mail, with no attempts yet. */
    public static LoginCode issue(String email, String codeHash, Instant now, Duration ttl) {
        return new LoginCode(email, codeHash, now, now.plus(ttl), 0, 0, null);
    }

    public boolean matches(String rawCode, PasswordEncoder encoder) {
        return rawCode != null && encoder.matches(rawCode, codeHash);
    }

    /** Whether the code can still be used at {@code now}; a block also ends the code. */
    public boolean isValidAt(Instant now) {
        return now.isBefore(expiresAt);
    }

    public boolean isBlockedAt(Instant now) {
        return blockedUntil != null && now.isBefore(blockedUntil);
    }

    public String email() {
        return email;
    }

    public String codeHash() {
        return codeHash;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public int attempts() {
        return attempts;
    }

    public int failures() {
        return failures;
    }

    public Instant blockedUntil() {
        return blockedUntil;
    }

    @Override
    public String toString() {
        return "LoginCode[email=" + email + ", createdAt=" + createdAt + ", expiresAt=" + expiresAt
                + ", attempts=" + attempts + ", failures=" + failures + ", blockedUntil=" + blockedUntil + "]";
    }
}
