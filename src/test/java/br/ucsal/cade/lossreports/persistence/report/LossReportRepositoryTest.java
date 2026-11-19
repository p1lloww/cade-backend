package br.ucsal.cade.lossreports.persistence.report;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.persistence.period.LostPeriod;
import br.ucsal.cade.lossreports.persistence.period.Shift;
import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class LossReportRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);
    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    @Autowired
    private LossReportRepository reports;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndReadsBackEveryField() {
        Category category = categories.saveAndFlush(new Category("Fone de teste", RiskLevel.MEDIUM));
        LostPeriod period = LostPeriod.of(TODAY.minusDays(3), TODAY.minusDays(1), Shift.AFTERNOON, TODAY);
        List<LossLocation> locations = List.of(LossLocation.of("Bloco 400", "Sala 421"),
                LossLocation.of("Bloco B", null));
        UUID student = UUID.randomUUID();

        UUID id = reports.saveAndFlush(LossReport.register(new NewLossReport(category, "Fone JBL preto",
                "Adesivo de estrela", locations, period, null), student, NOW)).getId();
        entityManager.clear();

        LossReport read = reports.findById(id).orElseThrow();
        assertThat(read.getStudentId()).isEqualTo(student);
        assertThat(read.getStatus()).isEqualTo(LossReportStatus.OPEN);
        assertThat(read.getPeriod()).isEqualTo(period);
        assertThat(read.getLocations()).containsExactlyElementsOf(locations);
        assertThat(read.getSecretDetail()).isEqualTo("Adesivo de estrela");
        assertThat(read.getExpiresAt()).isEqualTo(NOW.plus(LossReport.VALIDITY));
        assertThat(read.getVersion()).isZero();
    }

    @Test
    void savesAReportWithoutLocationAndWithoutShift() {
        Category category = categories.saveAndFlush(new Category("Celular de teste", RiskLevel.HIGH));
        LostPeriod period = LostPeriod.of(TODAY, null, null, TODAY);

        UUID id = reports.saveAndFlush(LossReport.register(new NewLossReport(category, "iPhone", null, List.of(),
                period, null), UUID.randomUUID(), NOW)).getId();
        entityManager.clear();

        LossReport read = reports.findById(id).orElseThrow();
        assertThat(read.getLocations()).isEmpty();
        assertThat(read.getPeriod().shift()).isNull();
    }

    @Test
    void countsOnlyTheActiveReportsOfTheStudent() {
        Category category = categories.saveAndFlush(new Category("Garrafa de teste", RiskLevel.LOW));
        UUID student = UUID.randomUUID();
        for (String status : List.of("OPEN", "AWAITING_INFO", "SUMMONED", "COMPLETED", "EXPIRED", "CANCELLED")) {
            UUID id = reports.saveAndFlush(report(category, student)).getId();
            entityManager.createNativeQuery("UPDATE loss_reports SET status = :status WHERE id = :id")
                    .setParameter("status", status).setParameter("id", id).executeUpdate();
        }
        reports.saveAndFlush(report(category, UUID.randomUUID()));

        assertThat(reports.countByStudentIdAndStatusIn(student, LossReportStatus.ACTIVE)).isEqualTo(3);
    }

    private static LossReport report(Category category, UUID student) {
        return LossReport.register(new NewLossReport(category, "Garrafa azul", null, List.of(),
                LostPeriod.of(TODAY, null, null, TODAY), null), student, NOW);
    }
}
