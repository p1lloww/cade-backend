package br.ucsal.cade.lossreports.service.report;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.lossreports.api.ComparableLossReport;
import br.ucsal.cade.lossreports.api.ComparableLossReports;
import br.ucsal.cade.lossreports.api.MatchableLossReport;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.persistence.period.Shift;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class ComparableLossReportsServiceTest {

    @Autowired
    private ComparableLossReports comparable;

    @Autowired
    private LossReportService lossReports;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private MutableClock clock;

    @Test
    void returnsTheStaffViewOfEachRequestedReport() {
        Category category = categories.findByNameIgnoreCase("Fone").orElseThrow();
        LocalDate today = LocalDate.now(clock);
        UUID report = lossReports.register(new RegisterLossReport(category.getId(), "Fone JBL preto",
                "Adesivo de estrela", List.of(LossLocation.of("Bloco 400", "Sala 421"), LossLocation.of("Bloco B",
                null)), today.minusDays(2), today, Shift.EVENING, null), UUID.randomUUID()).getId();

        Map<UUID, ComparableLossReport> found = comparable.findAll(List.of(report, UUID.randomUUID()));

        assertThat(found).containsOnlyKeys(report);
        ComparableLossReport view = found.get(report);
        assertThat(view.categoryName()).isEqualTo("Fone");
        assertThat(view.lostFrom()).isEqualTo(today.minusDays(2));
        assertThat(view.lostUntil()).isEqualTo(today);
        assertThat(view.shift()).isEqualTo("Noite");
        assertThat(view.locations()).containsExactly(new MatchableLossReport.Location("Bloco 400", "Sala 421"),
                new MatchableLossReport.Location("Bloco B", null));
        assertThat(view.description()).isEqualTo("Fone JBL preto");
        assertThat(view.secretDetail()).isEqualTo("Adesivo de estrela");
        assertThat(view.status()).isEqualTo("Aberto");
        assertThat(view.registeredAt()).isEqualTo(clock.instant());
    }

    @Test
    void returnsAnEmptyMapForNoIds() {
        assertThat(comparable.findAll(List.of())).isEmpty();
    }
}
