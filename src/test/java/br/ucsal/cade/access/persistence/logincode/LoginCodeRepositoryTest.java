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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class LoginCodeRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);

    @Autowired
    private LoginCodeRepository codes;

    @Test
    void findsOnlyTheLatestCodeOfTheEmail() {
        codes.saveAndFlush(LoginCode.issue("aluno@ucsal.edu.br", "hash-old", NOW, TTL));
        LoginCode latest = codes.saveAndFlush(LoginCode.issue("aluno@ucsal.edu.br", "hash-new", NOW.plusSeconds(90), TTL));
        codes.saveAndFlush(LoginCode.issue("outro@ucsal.edu.br", "hash-other", NOW.plusSeconds(120), TTL));

        assertThat(codes.findFirstByEmailOrderByCreatedAtDesc("aluno@ucsal.edu.br"))
                .get()
                .extracting(LoginCode::getId)
                .isEqualTo(latest.getId());
    }

    @Test
    void findsNothingForAnEmailWithoutCodes() {
        assertThat(codes.findFirstByEmailOrderByCreatedAtDesc("ninguem@ucsal.edu.br")).isEmpty();
    }
}
