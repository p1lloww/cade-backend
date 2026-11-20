package br.ucsal.cade.access.controller.user;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.access.persistence.user.UserRepository;
import br.ucsal.cade.administration.persistence.audit.AuditRecord;
import br.ucsal.cade.administration.persistence.audit.AuditRecordRepository;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.TestTokens;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private UserRepository users;

    @Autowired
    private AuditRecordRepository auditRecords;

    @Test
    void theAdministratorGrantsTheStaffRoleWithAReason() throws Exception {
        User student = users.save(User.newStudent(uniqueEmail(), clock.instant()));

        changeRole(student.getId(), "STAFF", "Contratada para a sala de achados.", Role.ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student.getId().toString()))
                .andExpect(jsonPath("$.email").value(student.getEmail()))
                .andExpect(jsonPath("$.role").value("STAFF"));

        assertThat(users.findById(student.getId())).get().extracting(User::getRole).isEqualTo(Role.STAFF);
        assertThat(auditRecords.findAll()).filteredOn(record -> record.getEntityId().equals(student.getId()))
                .singleElement().satisfies(record -> {
                    assertThat(record.getActorId()).isEqualTo(UUID.fromString(TestTokens.SUBJECT));
                    assertThat(record.getAction()).isEqualTo("STAFF_ROLE_GRANTED");
                    assertThat(record.getReason()).isEqualTo("Contratada para a sala de achados.");
                });
    }

    @Test
    void theAdministratorRevokesTheStaffRole() throws Exception {
        User staff = User.newStudent(uniqueEmail(), clock.instant());
        staff.grantStaff();
        User saved = users.save(staff);

        changeRole(saved.getId(), "STUDENT", "Saiu da sala de achados.", Role.ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        assertThat(auditRecords.findAll()).filteredOn(record -> record.getEntityId().equals(saved.getId()))
                .extracting(AuditRecord::getAction).containsExactly("STAFF_ROLE_REVOKED");
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"STUDENT", "STAFF"})
    void onlyTheAdministratorManagesAccounts(Role role) throws Exception {
        User student = users.save(User.newStudent(uniqueEmail(), clock.instant()));

        changeRole(student.getId(), "STAFF", "Tentativa.", role).andExpect(status().isForbidden());
        mvc.perform(get("/users").header("Authorization", bearer(role))).andExpect(status().isForbidden());

        assertThat(users.findById(student.getId())).get().extracting(User::getRole).isEqualTo(Role.STUDENT);
    }

    @Test
    void anAnonymousRequestIsUnauthorized() throws Exception {
        mvc.perform(get("/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void theAdministratorRoleCannotBeGivenThroughTheApi() throws Exception {
        User student = users.save(User.newStudent(uniqueEmail(), clock.instant()));

        changeRole(student.getId(), "ADMIN", "Tentativa.", Role.ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.assignableRole").value("O perfil deve ser STAFF ou STUDENT."));
    }

    @Test
    void theReasonIsRequired() throws Exception {
        User student = users.save(User.newStudent(uniqueEmail(), clock.instant()));

        changeRole(student.getId(), "STAFF", " ", Role.ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").value("Informe o motivo."));
    }

    @Test
    void anUnknownAccountIsNotFound() throws Exception {
        changeRole(UUID.randomUUID(), "STAFF", "Teste.", Role.ADMIN)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Conta não encontrada."));
    }

    @Test
    void grantingTheRoleTwiceIsAConflict() throws Exception {
        User staff = User.newStudent(uniqueEmail(), clock.instant());
        staff.grantStaff();
        User saved = users.save(staff);

        changeRole(saved.getId(), "STAFF", "De novo.", Role.ADMIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta conta já tem o perfil de funcionário."));
    }

    @Test
    void theAdministratorAccountIsAConflict() throws Exception {
        User admin = users.save(User.newAdmin(uniqueEmail(), clock.instant()));

        changeRole(admin.getId(), "STUDENT", "Teste.", Role.ADMIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("O perfil do administrador não pode ser alterado."));
    }

    @Test
    void theListFiltersByRoleAndEmailInPagesSortedByEmail() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        User first = User.newStudent("a-" + tag + "@ucsal.edu.br", clock.instant());
        first.grantStaff();
        users.save(first);
        User second = User.newStudent("b-" + tag + "@ucsal.edu.br", clock.instant());
        second.grantStaff();
        users.save(second);
        users.save(User.newStudent("c-" + tag + "@ucsal.edu.br", clock.instant()));

        mvc.perform(get("/users").header("Authorization", bearer(Role.ADMIN))
                        .param("role", "STAFF").param("email", tag.toUpperCase()).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].email").value("a-" + tag + "@ucsal.edu.br"))
                .andExpect(jsonPath("$.content[0].role").value("STAFF"))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    private ResultActions changeRole(UUID id, String role, String reason, Role caller) throws Exception {
        return mvc.perform(put("/users/{id}/role", id)
                .header("Authorization", bearer(caller))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "role": "%s", "reason": "%s" }""".formatted(role, reason)));
    }

    private String bearer(Role role) {
        return "Bearer " + TestTokens.forRole(encoder, clock.instant(), role);
    }

    private static String uniqueEmail() {
        return "conta-" + UUID.randomUUID() + "@ucsal.edu.br";
    }
}
