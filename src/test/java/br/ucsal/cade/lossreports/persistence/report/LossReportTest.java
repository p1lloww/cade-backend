package br.ucsal.cade.lossreports.persistence.report;

import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.persistence.period.LostPeriod;
import br.ucsal.cade.lossreports.persistence.period.Shift;
import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
                new NewLossReport(HEADPHONES, "  Fone JBL preto ", "   ", List.of(), PERIOD, null), STUDENT, NOW);

        assertThat(report.getDescription()).isEqualTo("Fone JBL preto");
        assertThat(report.getSecretDetail()).isNull();
    }

    @Test
    void keepsTheSecretDetail() {
        LossReport report = LossReport.register(new NewLossReport(HEADPHONES, "Fone JBL preto",
                " Adesivo de estrela ", List.of(), PERIOD, null), STUDENT, NOW);

        assertThat(report.getSecretDetail()).isEqualTo("Adesivo de estrela");
    }

    @Test
    void aReportWithoutLocationHasAnEmptyList() {
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
    void theLocationsCannotBeChangedFromOutside() {
        LossReport report = register(rooms(1));

        assertThatThrownBy(() -> report.getLocations().add(LossLocation.of("Bloco B", null)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void summoningAnOpenReportLocksTheDescription() {
        LossReport report = register(rooms(1));

        report.summon();

        assertThat(report.getStatus()).isEqualTo(LossReportStatus.SUMMONED);
        assertThat(report.getStatus().isEditable()).isFalse();
    }

    @Test
    void aSummonedReportCannotBeSummonedAgain() {
        LossReport report = register(rooms(1));
        report.summon();

        assertThatThrownBy(report::summon).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void anOpenReportCanBeEditedAndReportsWhatChanged() {
        LossReport report = register(rooms(1));
        Category bag = new Category("Mochila", RiskLevel.MEDIUM);
        LostPeriod other = LostPeriod.of(TODAY.minusDays(2), null, null, TODAY);

        List<String> changed = report.edit(bag, "Mochila preta", "Chaveiro vermelho", rooms(2), other);

        assertThat(changed).containsExactly("categoria", "descrição", "detalhe", "locais", "período");
        assertThat(report.getCategory()).isSameAs(bag);
        assertThat(report.getDescription()).isEqualTo("Mochila preta");
        assertThat(report.getSecretDetail()).isEqualTo("Chaveiro vermelho");
        assertThat(report.getLocations()).containsExactlyElementsOf(rooms(2));
        assertThat(report.getPeriod()).isEqualTo(other);
        assertThat(report.getStatus()).isEqualTo(LossReportStatus.OPEN);
    }

    @Test
    void anEditThatChangesNothingReportsNothing() {
        LossReport report = register(rooms(1));

        List<String> changed = report.edit(HEADPHONES, " Fone JBL preto ", "  ", rooms(1), PERIOD);

        assertThat(changed).isEmpty();
    }

    @Test
    void anEditStripsTheTextsDropsABlankDetailAndCountsRepeatedLocationsOnce() {
        LossReport report = register(rooms(1));
        report.edit(HEADPHONES, " Fone azul ", "Arranhão", List.of(), PERIOD);

        List<String> changed = report.edit(HEADPHONES, " Fone azul ", "   ",
                List.of(LossLocation.of("Bloco B", null), LossLocation.of("Bloco B", null)), PERIOD);

        assertThat(changed).containsExactly("detalhe", "locais");
        assertThat(report.getDescription()).isEqualTo("Fone azul");
        assertThat(report.getSecretDetail()).isNull();
        assertThat(report.getLocations()).containsExactly(LossLocation.of("Bloco B", null));
    }

    @Test
    void theExpiryDoesNotChangeWithAnEdit() {
        LossReport report = register(rooms(1));
        Instant expiresAt = report.getExpiresAt();

        report.edit(HEADPHONES, "Outro texto", null, rooms(1), PERIOD);

        assertThat(report.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void aReportThatIsNotOpenOrAwaitingInformationCannotBeEdited() {
        for (LossReportStatus locked : List.of(LossReportStatus.SUMMONED, LossReportStatus.COMPLETED,
                LossReportStatus.EXPIRED, LossReportStatus.CANCELLED, LossReportStatus.REFUSED)) {
            LossReport report = register(rooms(1));
            ReflectionTestUtils.setField(report, "status", locked);

            assertThatThrownBy(() -> report.edit(HEADPHONES, "Texto ajustado ao objeto", null, rooms(1), PERIOD))
                    .as("situação %s", locked.code())
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("editar um registro de perda")
                    .hasMessageContaining(locked.label());
            assertThat(report.getDescription()).isEqualTo("Fone JBL preto");
        }
    }

    @Test
    void aReportAwaitingInformationCanBeEdited() {
        LossReport report = register(rooms(1));
        ReflectionTestUtils.setField(report, "status", LossReportStatus.AWAITING_INFO);

        assertThat(report.edit(HEADPHONES, "Fone JBL preto, com estojo", null, rooms(1), PERIOD))
                .containsExactly("descrição");
    }

    @Test
    void aSummonedReportBecomesEditableAgainWhenTheSummonsExpires() {
        LossReport report = register(rooms(1));
        report.summon();
        report.release();

        assertThat(report.edit(HEADPHONES, "Fone JBL preto com estojo", null, rooms(1), PERIOD))
                .containsExactly("descrição");
    }

    private static List<LossLocation> rooms(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(n -> LossLocation.of("Bloco 400", "Sala 40" + n)).toList();
    }

    private static LossReport register(List<LossLocation> locations) {
        return LossReport.register(
                new NewLossReport(HEADPHONES, "Fone JBL preto", null, locations, PERIOD, null), STUDENT, NOW);
    }
}
