package br.ucsal.cade.access.persistence.logincode;

import br.ucsal.cade.access.persistence.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One-time login code sent to an institutional e-mail (RF01, D02, D03).
 *
 * <p>Only the hash of the code is stored. A code expires after a fixed time, can be used once and is locked after
 * a maximum number of wrong attempts. Only the most recent code of an e-mail is valid: requesting a new code makes
 * the previous one useless, because verification always looks up the latest.
 */
@Entity
@Table(name = "login_codes", indexes = @Index(name = "idx_login_codes_email_created_at",
        columnList = "email, createdAt"))
public class LoginCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    private Instant usedAt;

    /** Optimistic lock: two concurrent verifications cannot both use the code or skip an attempt. */
    @Version
    private long version;

    protected LoginCode() {
    }

    private LoginCode(String email, String codeHash, Instant createdAt, Instant expiresAt) {
        this.email = User.normalize(email);
        this.codeHash = Objects.requireNonNull(codeHash);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
    }

    /**
     * Creates a code for the given e-mail.
     *
     * @param codeHash hash of the raw code; the raw code itself is never stored
     * @param ttl      how long the code stays valid
     */
    public static LoginCode issue(String email, String codeHash, Instant now, Duration ttl) {
        return new LoginCode(email, codeHash, now, now.plus(ttl));
    }

    /** Whether this code was issued less than {@code interval} ago, which blocks sending another one. */
    public boolean wasIssuedWithin(Duration interval, Instant now) {
        return now.isBefore(createdAt.plus(interval));
    }

    /**
     * Checks a code typed by the user. A wrong code counts as an attempt; a right one marks this code as used.
     * The caller must save this entity even when the result is not {@link LoginCodeVerification#VALID}, so the
     * attempt counter is kept.
     */
    public LoginCodeVerification verify(String rawCode, PasswordEncoder encoder, Instant now, int maxAttempts) {
        if (usedAt != null) {
            return LoginCodeVerification.ALREADY_USED;
        }
        if (!now.isBefore(expiresAt)) {
            return LoginCodeVerification.EXPIRED;
        }
        if (attempts >= maxAttempts) {
            return LoginCodeVerification.TOO_MANY_ATTEMPTS;
        }
        if (rawCode == null || !encoder.matches(rawCode, codeHash)) {
            attempts++;
            return LoginCodeVerification.WRONG_CODE;
        }
        usedAt = now;
        return LoginCodeVerification.VALID;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
