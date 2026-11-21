package br.ucsal.cade.access.persistence.user;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.shared.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-13T12:00:00Z");
    private static final PageRequest BY_EMAIL = PageRequest.of(0, 20, Sort.by("email"));

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

    @Test
    void searchFiltersByRoleAndEmailPart() {
        User staff = User.newStudent("ana.lima@ucsal.edu.br", NOW);
        staff.grantStaff();
        users.saveAndFlush(staff);
        users.saveAndFlush(User.newStudent("ana.souza@ucsal.edu.br", NOW));
        users.saveAndFlush(User.newStudent("bruno@ucsal.edu.br", NOW));

        assertThat(users.search(Role.STAFF, "", BY_EMAIL)).extracting(User::getEmail)
                .containsExactly("ana.lima@ucsal.edu.br");
        assertThat(users.search(null, "ana", BY_EMAIL)).extracting(User::getEmail)
                .containsExactly("ana.lima@ucsal.edu.br", "ana.souza@ucsal.edu.br");
        assertThat(users.search(Role.STUDENT, "ana", BY_EMAIL)).extracting(User::getEmail)
                .containsExactly("ana.souza@ucsal.edu.br");
    }

    @Test
    void searchWithoutFiltersReturnsEveryAccountInPages() {
        users.saveAndFlush(User.newStudent("a@ucsal.edu.br", NOW));
        users.saveAndFlush(User.newStudent("b@ucsal.edu.br", NOW));
        users.saveAndFlush(User.newStudent("c@ucsal.edu.br", NOW));

        var firstPage = users.search(null, "", PageRequest.of(0, 2, Sort.by("email")));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent()).extracting(User::getEmail)
                .containsExactly("a@ucsal.edu.br", "b@ucsal.edu.br");
    }
}
