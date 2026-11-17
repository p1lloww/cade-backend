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
 * Código de login de uso único enviado a um e-mail institucional (RF01, D02, D03), na tabela {@code login_codes}
 * (D18).
 *
 * <p>Há no máximo uma linha por e-mail: um código novo sobrescreve o anterior, então a tabela nunca passa do número de
 * pessoas que já pediram um código e não precisa de limpeza. Uma linha vencida é só ignorada até o próximo código
 * sobrescrevê-la. Um login válido apaga a linha.
 *
 * <p>A linha também guarda, de um código para o outro, as tentativas erradas do e-mail e o fim de um bloqueio, para
 * que pedir um código novo não zere a proteção contra adivinhação (D18). As mudanças são feitas pelos comandos
 * atômicos do {@link LoginCodeStore}, nunca salvando esta entidade.
 */
@Entity
@Table(name = "login_codes")
public class LoginCode {

    /** E-mail normalizado para onde o código foi enviado. */
    @Id
    @Column(name = "email")
    private String email;

    /** Hash do código; o código puro nunca é guardado. */
    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Tentativas feitas com o código atual, certas ou erradas. */
    @Column(name = "attempts", nullable = false)
    private int attempts;

    /** Tentativas erradas do e-mail desde o último bloqueio ou login, somando todos os códigos. */
    @Column(name = "failures", nullable = false)
    private int failures;

    /** Até quando o e-mail não pode pedir código; {@code null} quando não está bloqueado. */
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

    /** Um código novo para o e-mail, ainda sem tentativas. */
    public static LoginCode issue(String email, String codeHash, Instant now, Duration ttl) {
        return new LoginCode(email, codeHash, now, now.plus(ttl), 0, 0, null);
    }

    public boolean matches(String rawCode, PasswordEncoder encoder) {
        return rawCode != null && encoder.matches(rawCode, codeHash);
    }

    /** Se o código ainda pode ser usado em {@code now}; um bloqueio também encerra o código. */
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
