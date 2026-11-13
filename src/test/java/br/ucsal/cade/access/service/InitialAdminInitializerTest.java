package br.ucsal.cade.access.service;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.access.persistence.User;
import br.ucsal.cade.access.persistence.UserRepository;
import br.ucsal.cade.shared.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "cade.initial-admin.email=Admin@ucsal.edu.br")
@Import(TestcontainersConfiguration.class)
class InitialAdminInitializerTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private InitialAdminInitializer initializer;

    @Autowired
    private Clock clock;

    @Test
    void createsTheAdminOnStartupAndDoesNotDuplicateItOnTheNextRun() {
        initializer.run(null);

        assertThat(users.findAll())
                .filteredOn(user -> user.getEmail().equals("admin@ucsal.edu.br"))
                .singleElement()
                .extracting(User::getRole)
                .isEqualTo(Role.ADMIN);
    }

    @Test
    void promotesAnExistingAccountInsteadOfCreatingAnother() {
        users.save(User.newStudent("promovido@ucsal.edu.br", Instant.parse("2026-11-13T12:00:00Z")));
        var other = new InitialAdminInitializer(users, clock, "promovido@ucsal.edu.br", "ucsal.edu.br");

        other.run(null);

        assertThat(users.findByEmail("promovido@ucsal.edu.br")).get()
                .extracting(User::getRole)
                .isEqualTo(Role.ADMIN);
    }

    @Test
    void refusesAnAdminEmailOutsideTheInstitutionalDomain() {
        var other = new InitialAdminInitializer(users, clock, "admin@gmail.com", "ucsal.edu.br");

        assertThatThrownBy(() -> other.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("@ucsal.edu.br");
    }
}
