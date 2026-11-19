package br.ucsal.cade.lossreports.persistence.report;

import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LossReportTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);
    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");
    private static final UUID STUDENT = UUID.randomUUID();
    private static final Category HEADPHONES = new Category("Fone", RiskLevel.MEDIUM);
    private static final LostPeriod PERIOD = LostPeriod.of(TODAY.minusDays(1), null, Shift.MORNING, TODAY);

    @Test
    void startsOpenAndExpiresInThirtyDays() {
        LossReport report = register(List.of(LossLocation.of("Bloco 400", "Sala 421")));

        assertThat(report.getStatus()).isEqualTo(LossReportStatus.OPEN);
        assertThat(report.getStudentId()).isEqualTo(STUDENT);
        assertThat(report.getRegisteredAt()).isEqualTo(NOW);
        assertThat(report.getExpiresAt()).isEqualTo(Instant.parse("2026-12-19T12:00:00Z"));
        assertThat(report.getPeriod()).isEqualTo(PERIOD);
        assertThat(report.getLocations()).containsExactly(new LossLocation("Bloco 400", "Sala 421"));
    }

    @Test
    void stripsTheTextsAndStoresABlankSecretDetailAsNull() {
        LossReport report = LossReport.register(
                new NewLossReport(HEADPHONES, "  Fone JBL preto ", "   ", List.of(), PERIOD), STUDENT, NOW);

        assertThat(report.getDescription()).isEqualTo("Fone JBL preto");
        assertThat(report.getSecretDetail()).isNull();
    }

    @Test
    void keepsTheSecretDetail() {
        LossReport report = LossReport.register(new NewLossReport(HEADPHONES, "Fone JBL preto",
                " Adesivo de estrela ", List.of(), PERIOD), STUDENT, NOW);

        assertThat(report.getSecretDetail()).isEqualTo("Adesivo de estrela");
    }

    @Test
    void aReportWithoutLocationHasAnEmptyList() {
        assertThat(register(null).getLocations()).isEmpty();
        assertThat(register(List.of()).getLocations()).isEmpty();
    }

    @Test
    void repeatedLocationsCountOnce() {
        LossReport report = register(List.of(LossLocation.of("Bloco 400", "Sala 421"),
                LossLocation.of(" Bloco 400", "Sala 421 "), LossLocation.of("Bloco 400", null)));

        assertThat(report.getLocations()).containsExactly(
                new LossLocation("Bloco 400", "Sala 421"), new LossLocation("Bloco 400", null));
    }

    @Test
    void acceptsUpToFiveDifferentLocationsAndRefusesMore() {
        assertThat(register(rooms(5)).getLocations()).hasSize(5);
        assertThatThrownBy(() -> register(rooms(6)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(LossReport.TOO_MANY_LOCATIONS_MESSAGE);
    }

    @Test
    void theLocationsCannotBeChangedFromOutside() {
        LossReport report = register(rooms(1));

        assertThatThrownBy(() -> report.getLocations().add(LossLocation.of("Bloco B", null)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static List<LossLocation> rooms(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(n -> LossLocation.of("Bloco 400", "Sala 40" + n)).toList();
    }

    private static LossReport register(List<LossLocation> locations) {
        return LossReport.register(
                new NewLossReport(HEADPHONES, "Fone JBL preto", null, locations, PERIOD), STUDENT, NOW);
    }
}
