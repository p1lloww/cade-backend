package br.ucsal.cade.lossreports.service.reporter;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.lossreports.api.StudentRefusals;
import br.ucsal.cade.lossreports.persistence.reporter.LossReporter;
import br.ucsal.cade.lossreports.persistence.reporter.StudentNotBlockedException;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import br.ucsal.cade.lossreports.service.report.StudentBlockedException;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.exception.ConflictException;
import br.ucsal.cade.shared.exception.NotFoundException;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class StudentReleaseServiceTest {

    @Autowired
    private StudentReleaseService service;

    @Autowired
    private LossReportService lossReports;

    @Autowired
    private StudentRefusals refusals;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

    private UUID admin;

    @BeforeEach
    void setUp() {
        admin = UUID.randomUUID();
    }

    @Test
    void aReleasedStudentRegistersLossesAgain() {
        UUID student = studentWithRefusals(LossReporter.MAX_REFUSALS);
        assertThatThrownBy(() -> register(student)).isInstanceOf(StudentBlockedException.class);

        LossReporter released = service.release(student, "Conversou com a sala e explicou o engano.", admin);

        assertThat(released.getRefusals()).isZero();
        assertThat(released.isBlocked()).isFalse();
        register(student);
        assertThat(refusalsOf(student)).isZero();
    }

    @Test
    void theReleaseGoesToTheHistoryWithTheAdministratorAndTheReason() {
        UUID student = studentWithRefusals(LossReporter.MAX_REFUSALS);

        service.release(student, "Conversou com a sala.", admin);

        assertThat(jdbc.queryForList("SELECT reason FROM audit_records WHERE actor_id = ? AND action = ? "
                        + "AND entity_id = ?", String.class, admin, StudentReleaseService.RELEASED_ACTION, student))
                .containsExactly("Estudante liberado após 3 recusas no balcão: Conversou com a sala.");
    }

    @Test
    void afterTheReleaseTheRefusalsCountAgainFromZero() {
        UUID student = studentWithRefusals(LossReporter.MAX_REFUSALS);
        service.release(student, "Liberado.", admin);

        transaction.executeWithoutResult(_ -> refusals.record(student, UUID.randomUUID()));

        assertThat(refusalsOf(student)).isOne();
    }

    @Test
    void listsOnlyTheBlockedStudentsWithTheMostRefusalsFirst() {
        UUID three = studentWithRefusals(3);
        UUID four = studentWithRefusals(4);
        UUID two = studentWithRefusals(2);

        assertThat(service.blocked()).extracting(LossReporter::getStudentId)
                .contains(four, three).doesNotContain(two)
                .containsSubsequence(four, three);
    }

    @Test
    void aStudentWhoIsNotBlockedIsNotReleased() {
        UUID student = studentWithRefusals(2);

        assertThatThrownBy(() -> service.release(student, "Liberado.", admin))
                .isInstanceOf(StudentNotBlockedException.class)
                .isInstanceOf(ConflictException.class)
                .hasMessageStartingWith("Este estudante não está bloqueado");

        assertThat(refusalsOf(student)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_records WHERE entity_id = ?", Long.class,
                student)).isZero();
    }

    @Test
    void anUnknownStudentIsNotFound() {
        assertThatThrownBy(() -> service.release(UUID.randomUUID(), "Liberado.", admin))
                .isInstanceOf(LossReporterNotFoundException.class)
                .isInstanceOf(NotFoundException.class);
    }

    private UUID studentWithRefusals(int count) {
        UUID student = UUID.randomUUID();
        jdbc.update("INSERT INTO loss_reporters (student_id, refusals, version) VALUES (?, ?, 0)", student, count);
        return student;
    }

    private int refusalsOf(UUID student) {
        return jdbc.queryForObject("SELECT refusals FROM loss_reporters WHERE student_id = ?", Integer.class,
                student);
    }

    private void register(UUID student) {
        lossReports.register(new RegisterLossReport(categories.findAll().getFirst().getId(), "Garrafa azul", null,
                null, LocalDate.now(clock), null, null, null), student);
    }
}
