package br.ucsal.cade.access.persistence.logincode;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LoginCodeTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);

    private final LoginCode code = LoginCode.issue(" Aluno@UCSAL.edu.br ", ENCODER.encode("123456"), NOW, TTL);

    @Test
    void startsWithANormalizedEmailNoAttemptsAndTheExpiryAfterTheTtl() {
        assertThat(code.email()).isEqualTo("aluno@ucsal.edu.br");
        assertThat(code.expiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(code.attempts()).isZero();
    }

    @Test
    void matchesOnlyTheRightCode() {
        assertThat(code.matches("123456", ENCODER)).isTrue();
        assertThat(code.matches("000000", ENCODER)).isFalse();
        assertThat(code.matches(null, ENCODER)).isFalse();
    }

    @Test
    void hidesTheHashInToString() {
        assertThat(code.toString()).doesNotContain(code.codeHash());
    }
}
