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
    private static final int MAX_ATTEMPTS = 5;

    private final LoginCode code = LoginCode.issue(" Aluno@UCSAL.edu.br ", ENCODER.encode("123456"), NOW, TTL);

    @Test
    void normalizesTheEmailAndExpiresAfterTheTtl() {
        assertThat(code.getEmail()).isEqualTo("aluno@ucsal.edu.br");
        assertThat(code.getExpiresAt()).isEqualTo(NOW.plus(TTL));
    }

    @Test
    void acceptsTheRightCodeOnceAndMarksItAsUsed() {
        Instant later = NOW.plusSeconds(30);

        assertThat(code.verify("123456", ENCODER, later, MAX_ATTEMPTS)).isEqualTo(LoginCodeVerification.VALID);
        assertThat(code.getUsedAt()).isEqualTo(later);
        assertThat(code.verify("123456", ENCODER, later, MAX_ATTEMPTS))
                .isEqualTo(LoginCodeVerification.ALREADY_USED);
    }

    @Test
    void countsWrongCodesAsAttempts() {
        assertThat(code.verify("000000", ENCODER, NOW, MAX_ATTEMPTS)).isEqualTo(LoginCodeVerification.WRONG_CODE);
        assertThat(code.verify(null, ENCODER, NOW, MAX_ATTEMPTS)).isEqualTo(LoginCodeVerification.WRONG_CODE);

        assertThat(code.getAttempts()).isEqualTo(2);
        assertThat(code.getUsedAt()).isNull();
    }

    @Test
    void locksAfterTheMaximumNumberOfAttemptsEvenForTheRightCode() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            code.verify("000000", ENCODER, NOW, MAX_ATTEMPTS);
        }

        assertThat(code.verify("123456", ENCODER, NOW, MAX_ATTEMPTS))
                .isEqualTo(LoginCodeVerification.TOO_MANY_ATTEMPTS);
        assertThat(code.getAttempts()).isEqualTo(MAX_ATTEMPTS);
    }

    @Test
    void expiresExactlyAtTheEndOfTheTtl() {
        assertThat(code.verify("123456", ENCODER, NOW.plus(TTL), MAX_ATTEMPTS))
                .isEqualTo(LoginCodeVerification.EXPIRED);
        assertThat(code.getUsedAt()).isNull();
    }

    @Test
    void blocksAResendWithinTheInterval() {
        Duration interval = Duration.ofMinutes(1);

        assertThat(code.wasIssuedWithin(interval, NOW.plusSeconds(59))).isTrue();
        assertThat(code.wasIssuedWithin(interval, NOW.plusSeconds(60))).isFalse();
    }
}
