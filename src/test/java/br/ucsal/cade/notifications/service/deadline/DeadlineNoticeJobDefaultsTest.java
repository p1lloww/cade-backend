package br.ucsal.cade.notifications.service.deadline;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import br.ucsal.cade.notifications.persistence.outbox.NotificationRepository;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Com a configuração padrão, o lembrete do registro de perda sai: o PER-06 faz o registro vencer e ser renovado (D30). */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class DeadlineNoticeJobDefaultsTest {

    @Autowired
    private DeadlineNoticeJob job;

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

    @Test
    void theLossReportReminderIsOnByDefault() {
        UUID category = categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.LOW)).getId();
        UUID report = lossReports.register(new RegisterLossReport(category, "Garrafa azul", null, List.of(),
                LocalDate.now(clock), null, null, null), UUID.randomUUID()).getId();
        jdbc.update("UPDATE loss_reports SET expires_at = ? WHERE id = ?",
                Timestamp.from(clock.instant().plus(Duration.ofDays(1))), report);

        job.queueDeadlineNotices();

        assertThat(notifications.findAll()).anyMatch(n -> n.getReferenceId().equals(report));
    }
}
