package br.ucsal.cade.access.persistence.user;

import br.ucsal.cade.shared.exception.RoleChangeNotAllowedException;
import br.ucsal.cade.shared.security.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");

    @Test
    void grantStaffTurnsAStudentIntoStaff() {
        User user = User.newStudent("estudante@ucsal.edu.br", NOW);

        user.grantStaff();

        assertThat(user.getRole()).isEqualTo(Role.STAFF);
    }

    @Test
    void grantStaffRefusesAnAccountThatIsAlreadyStaff() {
        User user = User.newStudent("funcionario@ucsal.edu.br", NOW);
        user.grantStaff();

        assertThatThrownBy(user::grantStaff)
                .isInstanceOf(RoleChangeNotAllowedException.class)
                .hasMessage("Esta conta já tem o perfil de funcionário.");
        assertThat(user.getRole()).isEqualTo(Role.STAFF);
    }

    @Test
    void revokeStaffTurnsStaffBackIntoAStudent() {
        User user = User.newStudent("funcionario@ucsal.edu.br", NOW);
        user.grantStaff();

        user.revokeStaff();

        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void revokeStaffRefusesAStudent() {
        User user = User.newStudent("estudante@ucsal.edu.br", NOW);

        assertThatThrownBy(user::revokeStaff)
                .isInstanceOf(RoleChangeNotAllowedException.class)
                .hasMessage("Esta conta não tem o perfil de funcionário.");
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void theAdministratorRoleNeverChanges() {
        User admin = User.newAdmin("admin@ucsal.edu.br", NOW);

        assertThatThrownBy(admin::grantStaff)
                .isInstanceOf(RoleChangeNotAllowedException.class)
                .hasMessage("O perfil do administrador não pode ser alterado.");
        assertThatThrownBy(admin::revokeStaff)
                .isInstanceOf(RoleChangeNotAllowedException.class)
                .hasMessage("O perfil do administrador não pode ser alterado.");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
    }
}
