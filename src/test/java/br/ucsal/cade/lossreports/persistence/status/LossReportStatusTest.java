package br.ucsal.cade.lossreports.persistence.status;

import br.ucsal.cade.shared.exception.ConflictException;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static br.ucsal.cade.lossreports.persistence.status.LossReportStatus.AWAITING_INFO;
import static br.ucsal.cade.lossreports.persistence.status.LossReportStatus.CANCELLED;
import static br.ucsal.cade.lossreports.persistence.status.LossReportStatus.COMPLETED;
import static br.ucsal.cade.lossreports.persistence.status.LossReportStatus.EXPIRED;
import static br.ucsal.cade.lossreports.persistence.status.LossReportStatus.OPEN;
import static br.ucsal.cade.lossreports.persistence.status.LossReportStatus.SUMMONED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** As transições da seção 6.2 dos requisitos (RN01, RN06, RNF13): todas as permitidas e todas as proibidas. */
class LossReportStatusTest {

    /** As sete ações, com o nome que aparece no relatório dos testes. */
    private static final Map<String, UnaryOperator<LossReportStatus>> ACTIONS = Map.of(
            "pedir informações sobre", LossReportStatus::requestInfo,
            "retomar", LossReportStatus::resume,
            "convocar", LossReportStatus::summon,
            "liberar", LossReportStatus::release,
            "concluir", LossReportStatus::complete,
            "vencer", LossReportStatus::expire,
            "cancelar", LossReportStatus::cancel);

    /** A tabela da seção 6.2: situação de origem, ação e situação de destino. */
    private static final List<Transition> ALLOWED = List.of(
            new Transition(OPEN, "pedir informações sobre", AWAITING_INFO),
            new Transition(AWAITING_INFO, "retomar", OPEN),
            new Transition(OPEN, "convocar", SUMMONED),
            new Transition(SUMMONED, "concluir", COMPLETED),
            new Transition(SUMMONED, "liberar", OPEN),
            new Transition(OPEN, "vencer", EXPIRED),
            new Transition(OPEN, "cancelar", CANCELLED),
            new Transition(AWAITING_INFO, "cancelar", CANCELLED));

    private record Transition(LossReportStatus from, String action, LossReportStatus to) {
    }

    static Stream<Arguments> allowedTransitions() {
        return ALLOWED.stream().map(t -> Arguments.of(
                Named.of(t.from().label(), t.from()), Named.of(t.action(), ACTIONS.get(t.action())), t.to()));
    }

    /** Toda combinação de situação e ação que não está na tabela. */
    static Stream<Arguments> forbiddenTransitions() {
        return LossReportStatus.ALL.stream().flatMap(from -> ACTIONS.keySet().stream()
                .filter(action -> ALLOWED.stream().noneMatch(t -> t.from().equals(from) && t.action().equals(action)))
                .map(action -> Arguments.of(Named.of(from.label(), from), action)));
    }

    @ParameterizedTest(name = "{0} → {1} → {2}")
    @MethodSource("allowedTransitions")
    void followsEveryAllowedTransition(LossReportStatus from, UnaryOperator<LossReportStatus> action,
                                       LossReportStatus expected) {
        assertThat(action.apply(from)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} não pode {1}")
    @MethodSource("forbiddenTransitions")
    void refusesEveryOtherTransition(LossReportStatus from, String action) {
        assertThatThrownBy(() -> ACTIONS.get(action).apply(from))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .isInstanceOf(ConflictException.class)
                .hasMessage("Não é possível " + action + " um registro de perda na situação " + from.label() + ".");
    }

    @Test
    void coversEveryCombinationOfStatusAndAction() {
        long combinations = (long) LossReportStatus.ALL.size() * ACTIONS.size();

        assertThat(ALLOWED.size() + forbiddenTransitions().count()).isEqualTo(combinations);
        assertThat(forbiddenTransitions().count()).isEqualTo(34);
    }

    @Test
    void aNewReportStartsOpen() {
        assertThat(LossReportStatus.initial()).isEqualTo(OPEN);
    }

    @Test
    void onlyCompletedExpiredAndCancelledAreFinal() {
        assertThat(LossReportStatus.ALL).filteredOn(LossReportStatus::isFinal)
                .containsExactly(COMPLETED, EXPIRED, CANCELLED);
    }

    @Test
    void aFinalStatusAllowsNoAction() {
        for (LossReportStatus status : List.of(COMPLETED, EXPIRED, CANCELLED)) {
            ACTIONS.values().forEach(action -> assertThatThrownBy(() -> action.apply(status))
                    .isInstanceOf(InvalidStatusTransitionException.class));
        }
    }

    @Test
    void openAwaitingInfoAndSummonedCountForTheLimitOfFive() {
        assertThat(LossReportStatus.ACTIVE).containsExactly(OPEN, AWAITING_INFO, SUMMONED);
    }

    @Test
    void onlyOpenAndAwaitingInfoCanBeEdited() {
        assertThat(LossReportStatus.ALL).filteredOn(LossReportStatus::isEditable)
                .containsExactly(OPEN, AWAITING_INFO);
    }

    @Test
    void aSummonedReportThatFailsAtTheCounterCanBeSummonedAgain() {
        assertThat(OPEN.summon().release().summon()).isEqualTo(SUMMONED);
    }

    @Test
    void theConverterStoresTheCodeAndReadsTheSameStatusBack() {
        LossReportStatusConverter converter = new LossReportStatusConverter();

        for (LossReportStatus status : LossReportStatus.ALL) {
            String column = converter.convertToDatabaseColumn(status);
            assertThat(column).isEqualTo(status.code()).matches("[A-Z_]+");
            assertThat(converter.convertToEntityAttribute(column)).isEqualTo(status);
        }
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void refusesAnUnknownCode() {
        assertThatThrownBy(() -> LossReportStatus.fromCode("LOST"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LOST");
    }
}
