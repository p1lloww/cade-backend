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
    void anOpenOrAwaitingReportCanBeCancelledAndTheOthersCannot() {
        for (LossReportStatus editable : List.of(LossReportStatus.OPEN, LossReportStatus.AWAITING_INFO)) {
            LossReport report = register(rooms(1));
            ReflectionTestUtils.setField(report, "status", editable);
            report.cancel();
            assertThat(report.getStatus()).isEqualTo(LossReportStatus.CANCELLED);
        }
        for (LossReportStatus other : List.of(LossReportStatus.SUMMONED, LossReportStatus.COMPLETED,
                LossReportStatus.EXPIRED, LossReportStatus.CANCELLED, LossReportStatus.REFUSED)) {
            LossReport report = register(rooms(1));
            ReflectionTestUtils.setField(report, "status", other);
            assertThatThrownBy(report::cancel).as(other.code()).isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Test
    void requestingInfoMovesAnOpenReportToAwaitingAndKeepsTheRequest() {
        LossReport report = register(rooms(1));
        UUID suggestion = UUID.randomUUID();

        report.requestInfo(suggestion, "Qual é a cor da capa?", NOW);

        assertThat(report.getStatus()).isEqualTo(LossReportStatus.AWAITING_INFO);
        assertThat(report.getInfoRequestReason()).isEqualTo("Qual é a cor da capa?");
        assertThat(report.getInfoRequestedAt()).isEqualTo(NOW);
        assertThat(report.getInfoSuggestionId()).isEqualTo(suggestion);
        assertThat(report.getStatus().isEditable()).isTrue();
    }

    @Test
    void anInfoRequestOnlyWorksOnAnOpenReport() {
        LossReport awaiting = register(rooms(1));
        awaiting.requestInfo(UUID.randomUUID(), "Pergunta", NOW);
        LossReport summoned = register(rooms(1));
        summoned.summon();

        assertThatThrownBy(() -> awaiting.requestInfo(UUID.randomUUID(), "Outra", NOW))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> summoned.requestInfo(UUID.randomUUID(), "Pergunta", NOW))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void answeringReturnsTheReportToOpenWithTheAnswerAndClosesTheRequest() {
        LossReport report = register(rooms(1));
        report.requestInfo(UUID.randomUUID(), "Qual é a cor da capa?", NOW);

        report.respond("  Azul, com um risco  ");

        assertThat(report.getStatus()).isEqualTo(LossReportStatus.OPEN);
        assertThat(report.getInfoAnswer()).isEqualTo("Azul, com um risco");
        assertThat(report.getInfoRequestReason()).isEqualTo("Qual é a cor da capa?");
        assertThat(report.getInfoRequestedAt()).isNull();
        assertThat(report.getInfoSuggestionId()).isNull();
    }

    @Test
    void onlyAReportAwaitingInformationCanBeAnswered() {
        assertThatThrownBy(() -> register(rooms(1)).respond("Resposta"))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void resumingWithoutAnswerReturnsToOpenAndKeepsNoAnswer() {
        LossReport report = register(rooms(1));
        report.requestInfo(UUID.randomUUID(), "Pergunta", NOW);

        report.resumeWithoutAnswer();

        assertThat(report.getStatus()).isEqualTo(LossReportStatus.OPEN);
        assertThat(report.getInfoAnswer()).isNull();
        assertThat(report.getInfoRequestedAt()).isNull();
    }

    @Test
    void anOpenReportExpiresAndNoOtherDoes() {
        LossReport report = register(rooms(1));
        report.expire();
        assertThat(report.getStatus()).isEqualTo(LossReportStatus.EXPIRED);

        LossReport awaiting = register(rooms(1));
        awaiting.requestInfo(UUID.randomUUID(), "Pergunta", NOW);
        LossReport summoned = register(rooms(1));
        summoned.summon();
        assertThatThrownBy(awaiting::expire).isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(summoned::expire).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void renewingStartsAnotherThirtyDaysFromNowWhileOpenOrAwaiting() {
        LossReport report = register(rooms(1));
        Instant later = NOW.plusSeconds(20 * 24 * 3600L);

        report.renew(later, LossReport.VALIDITY);

        assertThat(report.getExpiresAt()).isEqualTo(later.plus(LossReport.VALIDITY));
    }

    @Test
    void aReportThatIsNotOpenOrAwaitingCannotBeRenewed() {
        for (LossReportStatus locked : List.of(LossReportStatus.SUMMONED, LossReportStatus.COMPLETED,
                LossReportStatus.EXPIRED, LossReportStatus.CANCELLED, LossReportStatus.REFUSED)) {
            LossReport report = register(rooms(1));
            ReflectionTestUtils.setField(report, "status", locked);
            Instant expiresAt = report.getExpiresAt();

            assertThatThrownBy(() -> report.renew(NOW, LossReport.VALIDITY)).as(locked.code())
                    .isInstanceOf(InvalidStatusTransitionException.class);
            assertThat(report.getExpiresAt()).isEqualTo(expiresAt);
        }
    }

    private static List<LossLocation> rooms(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(n -> LossLocation.of("Bloco 400", "Sala 40" + n)).toList();
    }

    private static LossReport register(List<LossLocation> locations) {
        return LossReport.register(
                new NewLossReport(HEADPHONES, "Fone JBL preto", null, locations, PERIOD, null), STUDENT, NOW);
    }
}
