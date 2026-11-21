package br.ucsal.cade.access.service.user;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.access.persistence.user.RoleChangeNotAllowedException;
import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.access.persistence.user.UserRepository;
import br.ucsal.cade.administration.persistence.audit.AuditRecord;
import br.ucsal.cade.administration.persistence.audit.AuditRecordRepository;
import br.ucsal.cade.shared.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserRoleServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Autowired
    private UserRoleService service;

    @Autowired
    private UserRepository users;

    @Autowired
    private AuditRecordRepository records;

    @Test
    void grantingStaffChangesTheRoleAndRecordsTheReason() {
        User student = users.save(User.newStudent(uniqueEmail(), NOW));

        User changed = service.changeRole(student.getId(),
                new ChangeRole(Role.STAFF, "Contratada para a sala de achados."), ADMIN_ID);

        assertThat(changed.getRole()).isEqualTo(Role.STAFF);
        assertThat(users.findById(student.getId())).get().extracting(User::getRole).isEqualTo(Role.STAFF);
        List<AuditRecord> history = recordsFor(student.getId());
        assertThat(history).singleElement().satisfies(record -> {
            assertThat(record.getActorId()).isEqualTo(ADMIN_ID);
            assertThat(record.getAction()).isEqualTo(UserRoleService.GRANTED_ACTION);
            assertThat(record.getEntityType()).isEqualTo("User");
            assertThat(record.getReason()).isEqualTo("Contratada para a sala de achados.");
        });
    }

    @Test
    void revokingStaffTurnsTheAccountBackIntoAStudentAndRecordsIt() {
        User staff = User.newStudent(uniqueEmail(), NOW);
        staff.grantStaff();
        staff = users.save(staff);

        User changed = service.changeRole(staff.getId(),
                new ChangeRole(Role.STUDENT, "Saiu da sala de achados."), ADMIN_ID);

        assertThat(changed.getRole()).isEqualTo(Role.STUDENT);
        assertThat(recordsFor(staff.getId())).singleElement()
                .extracting(AuditRecord::getAction).isEqualTo(UserRoleService.REVOKED_ACTION);
    }

    @Test
    void aRefusedChangeKeepsTheRoleAndRecordsNothing() {
        User student = users.save(User.newStudent(uniqueEmail(), NOW));

        assertThatThrownBy(() -> service.changeRole(student.getId(),
                new ChangeRole(Role.STUDENT, "Engano."), ADMIN_ID))
                .isInstanceOf(RoleChangeNotAllowedException.class);

        assertThat(users.findById(student.getId())).get().extracting(User::getRole).isEqualTo(Role.STUDENT);
        assertThat(recordsFor(student.getId())).isEmpty();
    }

    @Test
    void theAdministratorAccountCannotLoseItsRole() {
        User admin = users.save(User.newAdmin(uniqueEmail(), NOW));

        assertThatThrownBy(() -> service.changeRole(admin.getId(),
                new ChangeRole(Role.STUDENT, "Teste."), ADMIN_ID))
                .isInstanceOf(RoleChangeNotAllowedException.class);

        assertThat(users.findById(admin.getId())).get().extracting(User::getRole).isEqualTo(Role.ADMIN);
        assertThat(recordsFor(admin.getId())).isEmpty();
    }

    @Test
    void anUnknownAccountIsNotFound() {
        assertThatThrownBy(() -> service.changeRole(UUID.randomUUID(),
                new ChangeRole(Role.STAFF, "Teste."), ADMIN_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void searchNormalizesTheEmailPart() {
        String email = uniqueEmail();
        users.save(User.newStudent(email, NOW));

        var found = service.search(null, "  " + email.toUpperCase() + " ", PageRequest.of(0, 20, Sort.by("email")));

        assertThat(found.getContent()).extracting(User::getEmail).containsExactly(email);
    }

    private List<AuditRecord> recordsFor(UUID userId) {
        return records.findAll().stream().filter(record -> record.getEntityId().equals(userId)).toList();
    }

    private static String uniqueEmail() {
        return "conta-" + UUID.randomUUID() + "@ucsal.edu.br";
    }
}
