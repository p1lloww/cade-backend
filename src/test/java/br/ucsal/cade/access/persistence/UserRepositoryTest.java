package br.ucsal.cade.access.persistence;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.shared.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-13T12:00:00Z");

    @Autowired
    private UserRepository users;

    @Test
    void savesANewStudentWithIdAndCreationDate() {
        User saved = users.saveAndFlush(User.newStudent("estudante@ucsal.edu.br", NOW));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(Role.STUDENT);
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void findsByNormalizedEmail() {
        users.saveAndFlush(User.newStudent("  Estudante@UCSAL.edu.br ", NOW));

        assertThat(users.findByEmail(User.normalize("ESTUDANTE@ucsal.edu.br")))
                .get()
                .extracting(User::getEmail)
                .isEqualTo("estudante@ucsal.edu.br");
    }

    @Test
    void rejectsTwoAccountsWithTheSameEmail() {
        users.saveAndFlush(User.newStudent("repetido@ucsal.edu.br", NOW));

        assertThatThrownBy(() -> users.saveAndFlush(User.newStudent("REPETIDO@ucsal.edu.br", NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
