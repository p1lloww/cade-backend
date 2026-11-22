package br.ucsal.cade.lossreports.controller.reporter;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.TestTokens;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class BlockedStudentsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theAdministratorSeesOnlyTheBlockedStudents() throws Exception {
        UUID blocked = studentWithRefusals(3);
        UUID notBlocked = studentWithRefusals(1);

        mvc.perform(get("/blocked-students").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].studentId", hasItem(blocked.toString())))
                .andExpect(jsonPath("$[*].studentId", not(hasItem(notBlocked.toString()))))
                .andExpect(jsonPath("$[?(@.studentId == '" + blocked + "')].refusals").value(3))
                .andExpect(jsonPath("$[?(@.studentId == '" + blocked + "')].blocked").value(true));
    }

    @Test
    void theAdministratorReleasesABlockedStudent() throws Exception {
        UUID student = studentWithRefusals(3);

        release(student, "Conversou com a sala.", Role.ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.toString()))
                .andExpect(jsonPath("$.refusals").value(0))
                .andExpect(jsonPath("$.blocked").value(false));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_records WHERE actor_id = ? AND action = ? "
                        + "AND entity_id = ?", Long.class, UUID.fromString(TestTokens.SUBJECT), "STUDENT_RELEASED",
                student)).isOne();
    }

    @Test
    void asksForTheReason() throws Exception {
        UUID student = studentWithRefusals(3);

        mvc.perform(post("/blocked-students/{id}/release", student)
                        .header("Authorization", bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").value("Informe o motivo da liberação."));
        release(student, " ", Role.ADMIN).andExpect(status().isBadRequest());
    }

    @Test
    void aStudentWhoIsNotBlockedIsAConflictAndAnUnknownOneIsNotFound() throws Exception {
        release(studentWithRefusals(2), "Liberado.", Role.ADMIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(startsWith(
                        "Este estudante não está bloqueado")));
        release(UUID.randomUUID(), "Liberado.", Role.ADMIN)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Estudante não encontrado entre os que registram perdas."));
    }

    @Test
    void onlyTheAdministratorReleases() throws Exception {
        UUID student = studentWithRefusals(3);

        for (Role role : new Role[]{Role.STAFF, Role.STUDENT}) {
            mvc.perform(get("/blocked-students").header("Authorization", bearer(role)))
                    .andExpect(status().isForbidden());
            release(student, "Liberado.", role).andExpect(status().isForbidden());
        }
        mvc.perform(get("/blocked-students")).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT refusals FROM loss_reporters WHERE student_id = ?", Integer.class,
                student)).isEqualTo(3);
    }

    private ResultActions release(UUID student, String reason, Role role) throws Exception {
        return mvc.perform(post("/blocked-students/{id}/release", student)
                .header("Authorization", bearer(role))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"" + reason + "\"}"));
    }

    private UUID studentWithRefusals(int count) {
        UUID student = UUID.randomUUID();
        jdbc.update("INSERT INTO loss_reporters (student_id, refusals, version) VALUES (?, ?, 0)", student, count);
        return student;
    }

    private String bearer(Role role) {
        return "Bearer " + TestTokens.forRole(encoder, clock.instant(), role);
    }
}
