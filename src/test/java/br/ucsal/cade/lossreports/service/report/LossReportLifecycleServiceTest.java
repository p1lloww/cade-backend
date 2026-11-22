package br.ucsal.cade.lossreports.service.report;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.lossreports.api.event.LossReportInfoRequestedEvent;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.persistence.report.LossReport;
import br.ucsal.cade.lossreports.persistence.report.LossReportRepository;
import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.matching.service.suggestion.SuggestionService;
import br.ucsal.cade.notifications.persistence.outbox.Notification;
import br.ucsal.cade.notifications.persistence.outbox.NotificationKind;
import br.ucsal.cade.notifications.persistence.outbox.NotificationRepository;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import br.ucsal.cade.shared.mail.EmailSender;
import br.ucsal.cade.shared.storage.TestPhotos;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * O ciclo de vida do registro de perda (PER-06) de ponta a ponta, com o relógio controlado: cancelar (RF17), pedir e
 * responder informações (RF18, RF24, RN13), renovar e vencer (RN14). A tarefa agendada roda só de hora em hora, então
 * aqui ela é chamada direto. Objetos e perdas ficam em categorias criadas só para cada teste, para o cruzamento
 * automático não gravar sugestões além das que o teste cria.
 */
@SpringBootTest
@RecordApplicationEvents
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class LossReportLifecycleServiceTest {

    @Autowired
    private LossReportLifecycleService lifecycle;

    @Autowired
    private LossReportExpiryJob job;

    @Autowired
    private LossReportService lossReportService;

    @Autowired
    private LossReportRepository lossReports;

    @Autowired
    private SuggestionService suggestionService;

    @Autowired
    private MatchSuggestionRepository suggestions;

    @Autowired
    private FoundItemService foundItemService;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private NotificationRepository notifications;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

    @Autowired
    private ApplicationEvents events;

    @MockitoBean
    private EmailSender emailSender;

    private UUID itemCategory;
    private UUID reportCategory;

    @BeforeEach
    void setUp() {
        when(emailSender.send(any())).thenReturn(true);
        itemCategory = newCategory();
        reportCategory = newCategory();
    }

    // ---- cancelar

    @Test
    void cancellingClosesTheReportAndItsOpenSuggestionsAndGoesToTheHistory() {
        UUID student = UUID.randomUUID();
        UUID report = lose(student);
        MatchSuggestion fresh = suggest(find(), report);
        MatchSuggestion onHold = suggest(find(), report);
        onHold.putOnHold();
        suggestions.saveAndFlush(onHold);
        MatchSuggestion discarded = suggest(find(), report);
        discarded.discard();
        suggestions.saveAndFlush(discarded);

        lifecycle.cancel(report, student);

        assertThat(statusOf(report)).isEqualTo(LossReportStatus.CANCELLED);
        assertThat(suggestionStatus(fresh)).isEqualTo(SuggestionStatus.CLOSED);
        assertThat(suggestionStatus(onHold)).isEqualTo(SuggestionStatus.CLOSED);
        assertThat(suggestionStatus(discarded)).isEqualTo(SuggestionStatus.DISCARDED);
        assertThat(actionsOf(report, student)).contains("LOSS_REPORT_CANCELLED");
        assertThat(actionsOf(fresh.getId(), AuditEvent.SYSTEM_ACTOR)).containsExactly("MATCH_CLOSED");
    }

    @Test
    void aSummonedOrFinishedReportCannotBeCancelledAndAnotherStudentSeesNotFound() {
        UUID student = UUID.randomUUID();
        UUID summoned = lose(student);
        jdbc.update("UPDATE loss_reports SET status = 'SUMMONED' WHERE id = ?", summoned);
        UUID mine = lose(student);

        assertThatThrownBy(() -> lifecycle.cancel(summoned, student))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> lifecycle.cancel(mine, UUID.randomUUID()))
                .isInstanceOf(LossReportNotFoundException.class);
        assertThat(statusOf(mine)).isEqualTo(LossReportStatus.OPEN);
    }

    // ---- pedir e responder informações

    @Test
    void asksForInfoFromANewSuggestionAndTheStudentIsEmailedWithoutTheObject() {
        UUID student = UUID.randomUUID();
        UUID report = lose(student);
        MatchSuggestion suggestion = suggest(find(), report);
        UUID staff = UUID.randomUUID();

        suggestionService.requestInfo(suggestion.getId(), "A capa tem alguma marca? Qual?", staff);

        LossReport awaiting = lossReports.findById(report).orElseThrow();
        assertThat(awaiting.getStatus()).isEqualTo(LossReportStatus.AWAITING_INFO);
        assertThat(awaiting.getInfoSuggestionId()).isEqualTo(suggestion.getId());
        assertThat(suggestionStatus(suggestion)).isEqualTo(SuggestionStatus.NEW);
        assertThat(actionsOf(report, staff)).containsExactly("LOSS_REPORT_INFO_REQUESTED");
        assertThat(events.stream(LossReportInfoRequestedEvent.class)).singleElement().satisfies(event -> {
            assertThat(event.lossReportId()).isEqualTo(report);
            assertThat(event.studentId()).isEqualTo(student);
        });
        assertThat(infoEmailsTo(student)).singleElement().satisfies(notification -> {
            assertThat(notification.getBody()).contains("A capa tem alguma marca? Qual?").contains("5 dias")
                    .doesNotContain("Fone");
        });
    }

    @Test
    void answeringReturnsTheReportToOpenAndTheAnswerStaysOnIt() {
        UUID student = UUID.randomUUID();
        UUID report = lose(student);
        suggestionService.requestInfo(suggest(find(), report).getId(), "Qual a cor da capa?", UUID.randomUUID());

        LossReport answered = lifecycle.respond(report, "Azul, com um risco na tampa", student);

        assertThat(answered.getStatus()).isEqualTo(LossReportStatus.OPEN);
        assertThat(lossReports.findById(report).orElseThrow().getInfoAnswer()).isEqualTo("Azul, com um risco na tampa");
        assertThat(actionsOf(report, student)).contains("LOSS_REPORT_INFO_ANSWERED");
        assertThat(jdbc.queryForList("SELECT reason FROM audit_records WHERE entity_id = ? AND action = ?",
                String.class, report, "LOSS_REPORT_INFO_ANSWERED")).noneMatch(reason -> reason.contains("Azul"));
    }

    @Test
    void afterFiveDaysWithoutAnswerTheReportReopensAndTheSuggestionIsDiscarded() {
        UUID report = lose();
        MatchSuggestion suggestion = suggest(find(), report);
        suggestionService.requestInfo(suggestion.getId(), "Qual a cor da capa?", UUID.randomUUID());

        clock.advance(Duration.ofDays(5).minusMinutes(1));
        job.applyDeadlines();
        assertThat(statusOf(report)).isEqualTo(LossReportStatus.AWAITING_INFO);

        clock.advance(Duration.ofMinutes(2));
        job.applyDeadlines();

        assertThat(statusOf(report)).isEqualTo(LossReportStatus.OPEN);
        assertThat(suggestionStatus(suggestion)).isEqualTo(SuggestionStatus.DISCARDED);
        assertThat(actionsOf(report, AuditEvent.SYSTEM_ACTOR)).containsExactly("LOSS_REPORT_INFO_OVERDUE");
        assertThat(actionsOf(suggestion.getId(), AuditEvent.SYSTEM_ACTOR)).containsExactly("MATCH_DISCARDED");
        assertThat(lossReports.findById(report).orElseThrow().getInfoAnswer()).isNull();
    }

    // ---- vencer e renovar

    @Test
    void anOpenReportExpiresAfterThirtyDaysAndItsSuggestionsAreClosed() {
        UUID report = lose();
        MatchSuggestion suggestion = suggest(find(), report);

        clock.advance(Duration.ofDays(30).minusMinutes(1));
        job.applyDeadlines();
        assertThat(statusOf(report)).isEqualTo(LossReportStatus.OPEN);

        clock.advance(Duration.ofMinutes(2));
        job.applyDeadlines();

        assertThat(statusOf(report)).isEqualTo(LossReportStatus.EXPIRED);
        assertThat(suggestionStatus(suggestion)).isEqualTo(SuggestionStatus.CLOSED);
        assertThat(actionsOf(report, AuditEvent.SYSTEM_ACTOR)).containsExactly("LOSS_REPORT_EXPIRED");
    }

    @Test
    void renewingBeforeTheDeadlineKeepsTheReportAliveForAnotherThirtyDays() {
        UUID student = UUID.randomUUID();
        UUID report = lose(student);
        clock.advance(Duration.ofDays(28));

        LossReport renewed = lifecycle.renew(report, student);

        assertThat(renewed.getExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofDays(30)));
        clock.advance(Duration.ofDays(5));
        job.applyDeadlines();
        assertThat(statusOf(report)).isEqualTo(LossReportStatus.OPEN);
        assertThat(actionsOf(report, student)).contains("LOSS_REPORT_RENEWED");
    }

    private LossReportStatus statusOf(UUID report) {
        return lossReports.findById(report).orElseThrow().getStatus();
    }

    private SuggestionStatus suggestionStatus(MatchSuggestion suggestion) {
        return suggestions.findById(suggestion.getId()).orElseThrow().getStatus();
    }

    private List<String> actionsOf(UUID entityId, UUID actor) {
        return jdbc.queryForList("SELECT action FROM audit_records WHERE entity_id = ? AND actor_id = ?",
                String.class, entityId, actor);
    }

    private List<Notification> infoEmailsTo(UUID student) {
        return notifications.findAll().stream()
                .filter(n -> n.getKind() == NotificationKind.INFO_REQUESTED && n.getRecipientId().equals(student))
                .toList();
    }

    private MatchSuggestion suggest(UUID item, UUID report) {
        return suggestions.saveAndFlush(MatchSuggestion.suggest(item, report, 80, clock.instant()));
    }

    private UUID find() {
        return foundItemService.register(new RegisterFoundItem(TestPhotos.jpeg(), itemCategory, "Bloco 400",
                "Sala 421", LocalDate.now(clock), null, null, "Fone preto", null), UUID.randomUUID()).getId();
    }

    private UUID lose() {
        return lose(UUID.randomUUID());
    }

    private UUID lose(UUID student) {
        return lossReportService.register(new RegisterLossReport(reportCategory, "Fone JBL preto", "Adesivo",
                List.of(LossLocation.of("Bloco 400", "Sala 421")), LocalDate.now(clock), null, null, null),
                student).getId();
    }

    private UUID newCategory() {
        return categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.MEDIUM)).getId();
    }
}
