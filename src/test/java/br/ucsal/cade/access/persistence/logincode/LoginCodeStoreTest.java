package br.ucsal.cade.access.persistence.logincode;

import br.ucsal.cade.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
@Import({TestcontainersConfiguration.class, LoginCodeStore.class})
class LoginCodeStoreTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);

    @Autowired
    private LoginCodeStore store;

    @Autowired
    private StringRedisTemplate redis;

    @Test
    void savesTheCodeWithAnExpirySoRedisDeletesItOnItsOwn() {
        store.save(LoginCode.issue("salvo@ucsal.edu.br", "hash", NOW, TTL), TTL);

        assertThat(store.find("salvo@ucsal.edu.br")).get()
                .satisfies(code -> {
                    assertThat(code.codeHash()).isEqualTo("hash");
                    assertThat(code.createdAt()).isEqualTo(NOW);
                    assertThat(code.expiresAt()).isEqualTo(NOW.plus(TTL));
                    assertThat(code.attempts()).isZero();
                });
        assertThat(redis.getExpire("cade:login-code:salvo@ucsal.edu.br", TimeUnit.SECONDS))
                .isBetween(TTL.toSeconds() - 5, TTL.toSeconds());
    }

    @Test
    void aNewCodeReplacesThePreviousOneAndResetsTheAttempts() {
        store.save(LoginCode.issue("troca@ucsal.edu.br", "old", NOW, TTL), TTL);
        store.registerAttempt("troca@ucsal.edu.br");

        store.save(LoginCode.issue("troca@ucsal.edu.br", "new", NOW.plusSeconds(60), TTL), TTL);

        assertThat(store.find("troca@ucsal.edu.br")).get()
                .satisfies(code -> {
                    assertThat(code.codeHash()).isEqualTo("new");
                    assertThat(code.attempts()).isZero();
                });
    }

    @Test
    void countsAttemptsOnlyWhileTheCodeExists() {
        store.save(LoginCode.issue("tentativa@ucsal.edu.br", "hash", NOW, TTL), TTL);

        assertThat(store.registerAttempt("tentativa@ucsal.edu.br")).isEqualTo(1);
        assertThat(store.registerAttempt("tentativa@ucsal.edu.br")).isEqualTo(2);
        assertThat(store.registerAttempt("sem-codigo@ucsal.edu.br")).isEqualTo(-1);
        assertThat(redis.hasKey("cade:login-code:sem-codigo@ucsal.edu.br")).isFalse();
    }

    @Test
    void consumeDeletesEverythingKeptForTheEmailOnlyOnce() {
        store.startCooldown("consumo@ucsal.edu.br", Duration.ofMinutes(1));
        store.save(LoginCode.issue("consumo@ucsal.edu.br", "hash", NOW, TTL), TTL);

        assertThat(store.consume("consumo@ucsal.edu.br")).isTrue();
        assertThat(store.consume("consumo@ucsal.edu.br")).isFalse();

        assertThat(store.find("consumo@ucsal.edu.br")).isEmpty();
        assertThat(redis.keys("cade:*consumo@ucsal.edu.br")).isEmpty();
    }

    @Test
    void theCooldownCanBeStartedOnlyOnceAndReportsTheTimeLeft() {
        Duration interval = Duration.ofMinutes(1);

        assertThat(store.startCooldown("espera@ucsal.edu.br", interval)).isTrue();
        assertThat(store.startCooldown("espera@ucsal.edu.br", interval)).isFalse();
        assertThat(store.cooldownRemaining("espera@ucsal.edu.br"))
                .isPositive()
                .isLessThanOrEqualTo(interval);
        assertThat(store.cooldownRemaining("livre@ucsal.edu.br")).isZero();
    }
}
