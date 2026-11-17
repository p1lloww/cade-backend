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
 * Códigos de login na tabela {@code login_codes} (D18). A tabela é criada pelo Hibernate a partir de
 * {@link LoginCode} (D06); esta classe só executa SQL nela.
 *
 * <p>Toda operação que confere uma regra e altera a linha é um único comando SQL. O PostgreSQL trava a linha durante o
 * comando, então duas requisições ao mesmo tempo não passam juntas por uma conferência: palpites em paralelo não
 * furam o limite de tentativas, e o mesmo código não é usado duas vezes. Os comandos rodam fora de qualquer transação
 * de propósito: uma tentativa recusada lança exceção, e um rollback desfaria a tentativa que acabou de ser contada.
 *
 * <p>A hora atual é sempre um parâmetro, vinda do {@code Clock} da aplicação, e nunca o {@code now()} do banco, para
 * os testes poderem adiantar o relógio e conferir expiração e bloqueio.
 */
@Repository
public class LoginCodeStore {

    /**
     * Insere o código, ou sobrescreve o código do e-mail, a não ser que o e-mail ainda esteja esperando: o último
     * código foi emitido há menos que o intervalo de reenvio, ou o e-mail está bloqueado. As tentativas erradas e os
     * bloqueios são mantidos.
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

    /** Conta uma tentativa num código que ainda vale e devolve o novo total; nenhuma linha quando não há código. */
    private static final String REGISTER_ATTEMPT = """
            UPDATE login_codes
               SET attempts = attempts + 1
             WHERE email = :email AND expires_at > :now
            RETURNING attempts
            """;

    /**
     * Conta uma tentativa errada do e-mail. Quando a contagem chega ao limite, bloqueia o e-mail, encerra o código
     * atual e recomeça a contagem. No lado direito do SET, o PostgreSQL sempre lê os valores de antes da alteração,
     * então todos os CASE veem o mesmo {@code failures}.
     */
    private static final String REGISTER_FAILURE = """
            UPDATE login_codes
               SET failures      = CASE WHEN failures + 1 >= :maxFailures THEN 0 ELSE failures + 1 END,
                   blocked_until = CASE WHEN failures + 1 >= :maxFailures THEN :blockedUntil ELSE blocked_until END,
                   expires_at    = CASE WHEN failures + 1 >= :maxFailures THEN :now ELSE expires_at END
             WHERE email = :email
            RETURNING blocked_until IS NOT NULL AND blocked_until > :now
            """;

    /** Apaga a linha só se ela ainda guarda o código que foi conferido. */
    private static final String CONSUME = """
            DELETE FROM login_codes
             WHERE email = :email AND code_hash = :codeHash
            """;

    private final JdbcClient jdbc;

    LoginCodeStore(DataSource dataSource) {
        this.jdbc = JdbcClient.create(dataSource);
    }

    /**
     * Grava um código novo, substituindo o anterior do mesmo e-mail.
     *
     * @param resendInterval tempo mínimo desde o código anterior do e-mail
     * @return {@code false} se nada foi gravado porque o e-mail ainda está esperando ou está bloqueado; quem chamou lê
     *         a linha com {@link #find(String)} para saber qual dos dois
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

    /** A linha do e-mail, mesmo com o código vencido; vazio se não houver. */
    public Optional<LoginCode> find(String email) {
        return jdbc.sql(FIND)
                .param("email", email)
                .query(LoginCodeStore::toLoginCode)
                .optional();
    }

    /**
     * Conta uma tentativa antes de o código ser comparado, para palpites em paralelo não verem todos a contagem antiga.
     *
     * @return o número de tentativas contando esta, ou vazio se o código não existe mais ou venceu
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
     * Conta uma tentativa errada do e-mail e o bloqueia quando a contagem chega a {@code maxFailures}.
     *
     * @return {@code true} se o e-mail ficou bloqueado depois desta tentativa
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
     * Apaga a linha do e-mail depois de um login válido, o que também zera as tentativas erradas.
     *
     * @return {@code true} só para a requisição que de fato apagou o código, então um código é usado uma vez mesmo
     *         quando duas requisições com o código certo chegam juntas
     */
    public boolean consume(String email, String codeHash) {
        return jdbc.sql(CONSUME)
                .param("email", email)
                .param("codeHash", codeHash)
                .update() == 1;
    }

    /** O driver do PostgreSQL aceita {@code OffsetDateTime}, não {@code Instant}. */
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
