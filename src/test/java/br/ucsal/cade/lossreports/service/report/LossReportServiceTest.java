package br.ucsal.cade.lossreports.service.report;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.lossreports.api.event.LossReportRegisteredEvent;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.persistence.period.Shift;
import br.ucsal.cade.lossreports.persistence.report.LossReport;
import br.ucsal.cade.lossreports.persistence.report.LossReportRepository;
import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.UnknownCategoryException;
import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.exception.ConflictException;
import br.ucsal.cade.shared.exception.TooManyActiveReportsException;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@RecordApplicationEvents
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class LossReportServiceTest {

    @Autowired
    private LossReportService service;

    @Autowired
    private LossReportRepository reports;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

    @Autowired
    private ApplicationEvents events;

    private Category category;
    private UUID student;

    @BeforeEach
    void setUp() {
        category = categories.findAll().getFirst();
        student = UUID.randomUUID();
    }

    @Test
    void registersAnOpenReportWithHistoryAndEvent() {
        LocalDate yesterday = today().minusDays(1);
        RegisterLossReport request = new RegisterLossReport(category.getId(), "Fone JBL preto", "Adesivo de estrela",
                List.of(LossLocation.of("Bloco 400", "Sala 421")), yesterday, null, Shift.EVENING);

        LossReport report = service.register(request, student);

        LossReport saved = reports.findById(report.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(LossReportStatus.OPEN);
        assertThat(saved.getStudentId()).isEqualTo(student);
        assertThat(saved.getRegisteredAt()).isEqualTo(clock.instant());
        assertThat(events.stream(AuditEvent.class)).containsExactly(new AuditEvent(student,
                LossReportService.REGISTERED_ACTION, "LossReport", report.getId(),
                LossReportService.REGISTERED_REASON));
        assertThat(events.stream(LossReportRegisteredEvent.class)).containsExactly(
                new LossReportRegisteredEvent(report.getId(), category.getId(), yesterday, yesterday));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_records WHERE entity_id = ?", Long.class,
                report.getId())).isOne();
    }

    @Test
    void refusesAnUnknownCategoryWithoutSavingAnything() {
        RegisterLossReport request = new RegisterLossReport(UUID.randomUUID(), "Fone", null, List.of(), today(),
                null, null);

        assertThatThrownBy(() -> service.register(request, student))
                .isInstanceOf(UnknownCategoryException.class)
                .isInstanceOf(BadRequestException.class);
        assertThat(activeReports()).isZero();
        assertThat(events.stream(LossReportRegisteredEvent.class)).isEmpty();
    }

    @Test
    void refusesTheSixthReportInProgress() {
        IntStream.range(0, LossReportService.MAX_ACTIVE_REPORTS).forEach(_ -> register());

        assertThatThrownBy(this::register)
                .isInstanceOf(TooManyActiveReportsException.class)
                .isInstanceOf(ConflictException.class)
                .hasMessageStartingWith("Você já tem 5 registros em andamento.");
        assertThat(activeReports()).isEqualTo(LossReportService.MAX_ACTIVE_REPORTS);
    }

    @Test
    void aFinishedReportFreesRoomForAnotherOne() {
        List<UUID> ids = IntStream.range(0, LossReportService.MAX_ACTIVE_REPORTS).mapToObj(_ -> register()).toList();
        jdbc.update("UPDATE loss_reports SET status = 'CANCELLED' WHERE id = ?", ids.getFirst());

        register();

        assertThat(activeReports()).isEqualTo(LossReportService.MAX_ACTIVE_REPORTS);
    }

    @Test
    void theLimitBelongsToEachStudent() {
        IntStream.range(0, LossReportService.MAX_ACTIVE_REPORTS).forEach(_ -> register());
        student = UUID.randomUUID();

        register();

        assertThat(activeReports()).isOne();
    }

    /**
     * O cenário da RN05 que o lock otimista resolve: com 4 registros, o estudante envia vários ao mesmo tempo. Só um
     * pode passar. Os outros falham pela contagem (se começaram depois do commit do primeiro) ou pela versão (se
     * contaram antes), e nenhum deixa registro gravado.
     */
    @Test
    void simultaneousRegistrationsNeverGoPastTheLimit() throws Exception {
        IntStream.range(0, LossReportService.MAX_ACTIVE_REPORTS - 1).forEach(_ -> register());

        List<Throwable> failures = registerAtTheSameTime(8);

        assertThat(failures).hasSize(7).allSatisfy(failure -> assertThat(failure)
                .isInstanceOfAny(ConflictException.class, ObjectOptimisticLockingFailureException.class));
        assertThat(activeReports()).isEqualTo(LossReportService.MAX_ACTIVE_REPORTS);
    }

    /** Sem a linha do estudante, o segundo {@code INSERT ... ON CONFLICT} espera o primeiro e segue sem erro. */
    @Test
    void simultaneousFirstRegistrationsOfANewStudentAllPass() throws Exception {
        assertThat(registerAtTheSameTime(2)).isEmpty();
        assertThat(activeReports()).isEqualTo(2);
    }

    /** Dispara os registros juntos, em Virtual Threads, e devolve as falhas. */
    private List<Throwable> registerAtTheSameTime(int count) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        Callable<UUID> task = () -> {
            start.await();
            return register();
        };
        List<Throwable> failures = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<UUID>> results = IntStream.range(0, count).mapToObj(_ -> executor.submit(task)).toList();
            start.countDown();
            for (Future<UUID> result : results) {
                try {
                    result.get();
                } catch (ExecutionException e) {
                    failures.add(e.getCause());
                }
            }
        }
        return failures;
    }

    private UUID register() {
        return service.register(new RegisterLossReport(category.getId(), "Garrafa azul", null, List.of(), today(),
                null, null), student).getId();
    }

    private long activeReports() {
        return reports.countByStudentIdAndStatusIn(student, LossReportStatus.ACTIVE);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
