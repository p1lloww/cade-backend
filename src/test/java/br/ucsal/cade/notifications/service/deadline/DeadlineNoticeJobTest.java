package br.ucsal.cade.notifications.service.deadline;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import br.ucsal.cade.notifications.persistence.outbox.Notification;
import br.ucsal.cade.notifications.persistence.outbox.NotificationKind;
import br.ucsal.cade.notifications.persistence.outbox.NotificationRepository;
import br.ucsal.cade.pickup.persistence.summons.NewSummons;
import br.ucsal.cade.pickup.persistence.summons.Summons;
import br.ucsal.cade.pickup.persistence.summons.SummonsRepository;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.mail.EmailSender;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * A tarefa roda sozinha só de hora em hora; nos testes ela é chamada direto. As convocações são gravadas com ids
 * soltos de objeto, perda e sugestão: não há chave estrangeira entre os módulos (D10). O lembrete do registro de perda,
 * desligado até o PER-06, é ligado aqui para ser testado.
 */
@SpringBootTest(properties = "cade.notifications.deadlines.loss-report-notice-enabled=true")
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class DeadlineNoticeJobTest {

    @Autowired
    private DeadlineNoticeJob job;

    @Autowired
    private SummonsRepository summonses;

    @Autowired
    private LossReportService lossReports;

    @Autowired
    private NotificationRepository notifications;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

    @MockitoBean
    private EmailSender emailSender;

    @BeforeEach
    void setUp() {
        when(emailSender.send(any())).thenReturn(true);
    }

    @Test
    void remindsOnceOfASummonsThatExpiresInLessThanADay() {
        Summons tomorrow = summonsExpiringIn(Duration.ofHours(20));
        Summons later = summonsExpiringIn(Duration.ofDays(3));

        job.queueDeadlineNotices();
        job.queueDeadlineNotices();

        assertThat(notificationsOf(tomorrow.getId())).singleElement().satisfies(notification -> {
            assertThat(notification.getKind()).isEqualTo(NotificationKind.SUMMONS_EXPIRING);
            assertThat(notification.getRecipientId()).isEqualTo(tomorrow.getStudentId());
            assertThat(notification.getSubject()).isEqualTo(SummonsExpiringNotice.SUBJECT);
            assertThat(notification.getBody()).contains("Sala de achados e perdidos da UCSal");
        });
        assertThat(notificationsOf(later.getId())).isEmpty();
    }

    @Test
    void theReminderComesWhenTheSummonsEntersTheWindow() {
        Summons summons = summonsExpiringIn(Duration.ofHours(30));
        job.queueDeadlineNotices();
        assertThat(notificationsOf(summons.getId())).isEmpty();

        clock.advance(Duration.ofHours(7));
        job.queueDeadlineNotices();

        assertThat(notificationsOf(summons.getId())).hasSize(1);
    }

    @Test
    void aSummonsThatIsOverOrAlreadyExpiredIsNotReminded() {
        Summons completed = summonsExpiringIn(Duration.ofHours(10));
        completed.complete(UUID.randomUUID(), clock.instant());
        summonses.saveAndFlush(completed);
        Summons expired = summonsExpiringIn(Duration.ofHours(10));
        clock.advance(Duration.ofHours(11));

        job.queueDeadlineNotices();

        assertThat(notificationsOf(completed.getId())).isEmpty();
        assertThat(notificationsOf(expired.getId())).isEmpty();
    }

    @Test
    void remindsOnceOfAnOpenLossReportCloseToExpiring() {
        UUID student = UUID.randomUUID();
        UUID soon = lossReportExpiringIn(student, Duration.ofDays(2));
        UUID later = lossReportExpiringIn(UUID.randomUUID(), Duration.ofDays(10));

        job.queueDeadlineNotices();
        job.queueDeadlineNotices();

        assertThat(notificationsOf(soon)).singleElement().satisfies(notification -> {
            assertThat(notification.getKind()).isEqualTo(NotificationKind.LOSS_REPORT_EXPIRING);
            assertThat(notification.getRecipientId()).isEqualTo(student);
            assertThat(notification.getBody()).contains("\"Garrafa azul de teste\"");
        });
        assertThat(notificationsOf(later)).isEmpty();
    }

    @Test
    void aSummonedOrCancelledLossReportIsNotReminded() {
        UUID summoned = lossReportExpiringIn(UUID.randomUUID(), Duration.ofDays(2));
        jdbc.update("UPDATE loss_reports SET status = 'SUMMONED' WHERE id = ?", summoned);
        UUID cancelled = lossReportExpiringIn(UUID.randomUUID(), Duration.ofDays(2));
        jdbc.update("UPDATE loss_reports SET status = 'CANCELLED' WHERE id = ?", cancelled);

        job.queueDeadlineNotices();

        assertThat(notificationsOf(summoned)).isEmpty();
        assertThat(notificationsOf(cancelled)).isEmpty();
    }

    private Summons summonsExpiringIn(Duration left) {
        Instant now = clock.instant();
        return summonses.saveAndFlush(Summons.issue(new NewSummons(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), RiskLevel.LOW), now.minusSeconds(1),
                now.plus(left)));
    }

    private UUID lossReportExpiringIn(UUID student, Duration left) {
        UUID category = categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.LOW)).getId();
        UUID id = lossReports.register(new RegisterLossReport(category, "Garrafa azul de teste", null, List.of(),
                LocalDate.now(clock), null, null, null), student).getId();
        jdbc.update("UPDATE loss_reports SET expires_at = ? WHERE id = ?",
                Timestamp.from(clock.instant().plus(left)), id);
        return id;
    }

    private List<Notification> notificationsOf(UUID reference) {
        return notifications.findAll().stream().filter(n -> n.getReferenceId().equals(reference)).toList();
    }
}
