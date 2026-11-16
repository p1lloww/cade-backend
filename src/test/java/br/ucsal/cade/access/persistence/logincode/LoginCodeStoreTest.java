package br.ucsal.cade.access.persistence.logincode;

import br.ucsal.cade.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** O SQL do {@link LoginCodeStore} contra um PostgreSQL de verdade, com a tabela criada pelo Hibernate a partir da entidade. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, LoginCodeStore.class})
class LoginCodeStoreTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Duration RESEND = Duration.ofMinutes(1);
    private static final Duration BLOCK = Duration.ofMinutes(15);

    @Autowired
    private LoginCodeStore store;

    @Test
    void savesAndFindsTheCode() {
        assertThat(store.save(LoginCode.issue("salvo@ucsal.edu.br", "hash", NOW, TTL), RESEND)).isTrue();

        assertThat(store.find("salvo@ucsal.edu.br")).get()
                .satisfies(code -> {
                    assertThat(code.codeHash()).isEqualTo("hash");
                    assertThat(code.createdAt()).isEqualTo(NOW);
                    assertThat(code.expiresAt()).isEqualTo(NOW.plus(TTL));
                    assertThat(code.attempts()).isZero();
                    assertThat(code.blockedUntil()).isNull();
                });
        assertThat(store.find("outro@ucsal.edu.br")).isEmpty();
    }

    @Test
    void refusesANewCodeBeforeTheResendIntervalAndAcceptsItAfter() {
        store.save(LoginCode.issue("espera@ucsal.edu.br", "old", NOW, TTL), RESEND);

        assertThat(store.save(LoginCode.issue("espera@ucsal.edu.br", "early", NOW.plusSeconds(59), TTL), RESEND))
                .isFalse();
        assertThat(store.find("espera@ucsal.edu.br")).get().extracting(LoginCode::codeHash).isEqualTo("old");

        assertThat(store.save(LoginCode.issue("espera@ucsal.edu.br", "new", NOW.plus(RESEND), TTL), RESEND))
                .isTrue();
    }

    @Test
    void aNewCodeReplacesThePreviousOneAndResetsItsAttemptsButNotTheFailures() {
        store.save(LoginCode.issue("troca@ucsal.edu.br", "old", NOW, TTL), RESEND);
        store.registerAttempt("troca@ucsal.edu.br", NOW);
        store.registerFailure("troca@ucsal.edu.br", NOW, 15, BLOCK);

        store.save(LoginCode.issue("troca@ucsal.edu.br", "new", NOW.plus(RESEND), TTL), RESEND);

        assertThat(store.find("troca@ucsal.edu.br")).get()
                .satisfies(code -> {
                    assertThat(code.codeHash()).isEqualTo("new");
                    assertThat(code.attempts()).isZero();
                    assertThat(code.failures()).isEqualTo(1);
                });
    }

    @Test
    void countsAttemptsOnlyWhileTheCodeIsValid() {
        store.save(LoginCode.issue("tentativa@ucsal.edu.br", "hash", NOW, TTL), RESEND);

        assertThat(store.registerAttempt("tentativa@ucsal.edu.br", NOW)).hasValue(1);
        assertThat(store.registerAttempt("tentativa@ucsal.edu.br", NOW)).hasValue(2);
        assertThat(store.registerAttempt("tentativa@ucsal.edu.br", NOW.plus(TTL))).isEmpty();
        assertThat(store.registerAttempt("sem-codigo@ucsal.edu.br", NOW)).isEmpty();
    }

    @Test
    void blocksTheEmailWhenTheFailuresReachTheLimitAndEndsTheCurrentCode() {
        store.save(LoginCode.issue("bloqueio@ucsal.edu.br", "hash", NOW, TTL), RESEND);

        assertThat(store.registerFailure("bloqueio@ucsal.edu.br", NOW, 3, BLOCK)).isFalse();
        assertThat(store.registerFailure("bloqueio@ucsal.edu.br", NOW, 3, BLOCK)).isFalse();
        assertThat(store.registerFailure("bloqueio@ucsal.edu.br", NOW, 3, BLOCK)).isTrue();

        assertThat(store.find("bloqueio@ucsal.edu.br")).get()
                .satisfies(code -> {
                    assertThat(code.blockedUntil()).isEqualTo(NOW.plus(BLOCK));
                    assertThat(code.failures()).isZero();
                    assertThat(code.isValidAt(NOW)).isFalse();
                });
    }

    @Test
    void aBlockedEmailCannotGetANewCodeUntilTheBlockEnds() {
        store.save(LoginCode.issue("bloqueado@ucsal.edu.br", "hash", NOW, TTL), RESEND);
        store.registerFailure("bloqueado@ucsal.edu.br", NOW, 1, BLOCK);

        assertThat(store.save(LoginCode.issue("bloqueado@ucsal.edu.br", "new", NOW.plus(RESEND), TTL), RESEND))
                .isFalse();
        assertThat(store.save(LoginCode.issue("bloqueado@ucsal.edu.br", "new", NOW.plus(BLOCK), TTL), RESEND))
                .isTrue();
    }

    @Test
    void consumeDeletesTheCodeOnlyOnceAndOnlyIfItIsTheOneThatWasChecked() {
        store.save(LoginCode.issue("consumo@ucsal.edu.br", "hash", NOW, TTL), RESEND);

        assertThat(store.consume("consumo@ucsal.edu.br", "other-hash")).isFalse();
        assertThat(store.consume("consumo@ucsal.edu.br", "hash")).isTrue();
        assertThat(store.consume("consumo@ucsal.edu.br", "hash")).isFalse();

        assertThat(store.find("consumo@ucsal.edu.br")).isEmpty();
    }
}
