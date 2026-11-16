package br.ucsal.cade.access.persistence.logincode;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Login codes in the {@code login_codes} table (D18). The table itself is created by Hibernate from
 * {@link LoginCode} (D06); this class only runs SQL on it.
 *
 * <p>Every operation that checks a rule and changes the row is a single SQL statement. PostgreSQL locks the row while
 * the statement runs, so two concurrent requests cannot both pass a check: parallel guesses cannot go past the
 * attempt limit, and the same code cannot be used twice. The statements run outside any surrounding transaction on
 * purpose: a refused attempt throws an exception, and a rollback would undo the attempt it just counted.
 *
 * <p>The current time is always a parameter, taken from the application {@code Clock}, and never {@code now()} of
 * the database, so tests can move the clock forward to check expiry and blocks.
 */
@Repository
public class LoginCodeStore {

    /**
     * Inserts the code, or overwrites the code of the e-mail, unless the e-mail is still waiting: the last code was
     * issued less than the resend interval ago, or the e-mail is blocked. Wrong attempts and blocks are kept.
     */
    private static final String SAVE = """
            INSERT INTO login_codes (email, code_hash, created_at, expires_at, attempts, failures, blocked_until)
            VALUES (:email, :codeHash, :createdAt, :expiresAt, 0, 0, NULL)
            ON CONFLICT (email) DO UPDATE
               SET code_hash = EXCLUDED.code_hash,
                   created_at = EXCLUDED.created_at,
                   expires_at = EXCLUDED.expires_at,
                   attempts = 0
             WHERE login_codes.created_at <= :resendAllowedFrom
               AND (login_codes.blocked_until IS NULL OR login_codes.blocked_until <= :createdAt)
            """;

    private static final String FIND = """
            SELECT email, code_hash, created_at, expires_at, attempts, failures, blocked_until
              FROM login_codes
             WHERE email = :email
            """;

    /** Counts one attempt on a code that is still valid and returns the new total; no row when there is none. */
    private static final String REGISTER_ATTEMPT = """
            UPDATE login_codes
               SET attempts = attempts + 1
             WHERE email = :email AND expires_at > :now
            RETURNING attempts
            """;

    /**
     * Counts one wrong attempt for the e-mail. When the count reaches the limit, it blocks the e-mail, ends the
     * current code and starts the count again. On the right side of SET, PostgreSQL always reads the values from
     * before the update, so every CASE sees the same {@code failures}.
     */
    private static final String REGISTER_FAILURE = """
            UPDATE login_codes
               SET failures      = CASE WHEN failures + 1 >= :maxFailures THEN 0 ELSE failures + 1 END,
                   blocked_until = CASE WHEN failures + 1 >= :maxFailures THEN :blockedUntil ELSE blocked_until END,
                   expires_at    = CASE WHEN failures + 1 >= :maxFailures THEN :now ELSE expires_at END
             WHERE email = :email
            RETURNING blocked_until IS NOT NULL AND blocked_until > :now
            """;

    /** Deletes the row only if it still holds the code that was checked. */
    private static final String CONSUME = """
            DELETE FROM login_codes
             WHERE email = :email AND code_hash = :codeHash
            """;

    private final JdbcClient jdbc;

    LoginCodeStore(DataSource dataSource) {
        this.jdbc = JdbcClient.create(dataSource);
    }

    /**
     * Stores a new code, replacing the previous one of the same e-mail.
     *
     * @param resendInterval minimum time since the previous code of the e-mail
     * @return {@code false} if nothing was stored because the e-mail is still waiting or is blocked; the caller reads
     *         the row with {@link #find(String)} to tell which
     */
    public boolean save(LoginCode code, Duration resendInterval) {
        return jdbc.sql(SAVE)
                .param("email", code.email())
                .param("codeHash", code.codeHash())
                .param("createdAt", utc(code.createdAt()))
                .param("expiresAt", utc(code.expiresAt()))
                .param("resendAllowedFrom", utc(code.createdAt().minus(resendInterval)))
                .update() == 1;
    }

    /** The row of the e-mail, even if its code has expired; empty if there is none. */
    public Optional<LoginCode> find(String email) {
        return jdbc.sql(FIND)
                .param("email", email)
                .query(LoginCodeStore::toLoginCode)
                .optional();
    }

    /**
     * Counts one attempt before the code is compared, so parallel guesses cannot all see the old count.
     *
     * @return the number of attempts including this one, or empty if the code no longer exists or has expired
     */
    public OptionalInt registerAttempt(String email, Instant now) {
        return jdbc.sql(REGISTER_ATTEMPT)
                .param("email", email)
                .param("now", utc(now))
                .query(Integer.class)
                .optional()
                .map(OptionalInt::of)
                .orElse(OptionalInt.empty());
    }

    /**
     * Counts one wrong attempt for the e-mail and blocks it once the count reaches {@code maxFailures}.
     *
     * @return {@code true} if the e-mail is blocked after this attempt
     */
    public boolean registerFailure(String email, Instant now, int maxFailures, Duration blockDuration) {
        return jdbc.sql(REGISTER_FAILURE)
                .param("email", email)
                .param("now", utc(now))
                .param("maxFailures", maxFailures)
                .param("blockedUntil", utc(now.plus(blockDuration)))
                .query(Boolean.class)
                .optional()
                .orElse(false);
    }

    /**
     * Deletes the row of the e-mail after a successful login, which also clears its wrong attempts.
     *
     * @return {@code true} only for the request that actually deleted the code, so a code is used once even when
     *         two requests with the right code arrive together
     */
    public boolean consume(String email, String codeHash) {
        return jdbc.sql(CONSUME)
                .param("email", email)
                .param("codeHash", codeHash)
                .update() == 1;
    }

    /** The PostgreSQL driver takes {@code OffsetDateTime}, not {@code Instant}. */
    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        OffsetDateTime value = row.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static LoginCode toLoginCode(ResultSet row, int rowNumber) throws SQLException {
        return new LoginCode(row.getString("email"), row.getString("code_hash"),
                instant(row, "created_at"), instant(row, "expires_at"),
                row.getInt("attempts"), row.getInt("failures"), instant(row, "blocked_until"));
    }
}
