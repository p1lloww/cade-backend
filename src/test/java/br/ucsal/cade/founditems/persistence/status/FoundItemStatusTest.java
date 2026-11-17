package br.ucsal.cade.founditems.persistence.status;

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

import static br.ucsal.cade.founditems.persistence.status.FoundItemStatus.AVAILABLE;
import static br.ucsal.cade.founditems.persistence.status.FoundItemStatus.DISPOSED;
import static br.ucsal.cade.founditems.persistence.status.FoundItemStatus.RESERVED;
import static br.ucsal.cade.founditems.persistence.status.FoundItemStatus.RETURNED;
import static br.ucsal.cade.founditems.persistence.status.FoundItemStatus.UNCLAIMED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** As transições da seção 6.1 dos requisitos (RF12, RN01, RNF13): todas as permitidas e todas as proibidas. */
class FoundItemStatusTest {

    /** As cinco ações, com o nome que aparece no relatório dos testes. */
    private static final Map<String, UnaryOperator<FoundItemStatus>> ACTIONS = Map.of(
            "convocar", FoundItemStatus::reserve,
            "liberar", FoundItemStatus::release,
            "entregar", FoundItemStatus::handOver,
            "marcar como não reclamado", FoundItemStatus::markUnclaimed,
            "destinar", FoundItemStatus::dispose);

    /** A tabela da seção 6.1: situação de origem, ação e situação de destino. */
    private static final List<Transition> ALLOWED = List.of(
            new Transition(AVAILABLE, "convocar", RESERVED),
            new Transition(RESERVED, "liberar", AVAILABLE),
            new Transition(RESERVED, "entregar", RETURNED),
            new Transition(AVAILABLE, "entregar", RETURNED),
            new Transition(AVAILABLE, "marcar como não reclamado", UNCLAIMED),
            new Transition(UNCLAIMED, "convocar", RESERVED),
            new Transition(UNCLAIMED, "entregar", RETURNED),
            new Transition(UNCLAIMED, "destinar", DISPOSED));

    private record Transition(FoundItemStatus from, String action, FoundItemStatus to) {
    }

    static Stream<Arguments> allowedTransitions() {
        return ALLOWED.stream().map(t -> Arguments.of(
                Named.of(t.from().label(), t.from()), Named.of(t.action(), ACTIONS.get(t.action())), t.to()));
    }

    /** Toda combinação de situação e ação que não está na tabela. */
    static Stream<Arguments> forbiddenTransitions() {
        return FoundItemStatus.ALL.stream().flatMap(from -> ACTIONS.keySet().stream()
                .filter(action -> ALLOWED.stream().noneMatch(t -> t.from().equals(from) && t.action().equals(action)))
                .map(action -> Arguments.of(Named.of(from.label(), from), action)));
    }

    @ParameterizedTest(name = "{0} → {1} → {2}")
    @MethodSource("allowedTransitions")
    void followsEveryAllowedTransition(FoundItemStatus from, UnaryOperator<FoundItemStatus> action,
                                       FoundItemStatus expected) {
        assertThat(action.apply(from)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} não pode {1}")
    @MethodSource("forbiddenTransitions")
    void refusesEveryOtherTransition(FoundItemStatus from, String action) {
        assertThatThrownBy(() -> ACTIONS.get(action).apply(from))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .isInstanceOf(ConflictException.class)
                .hasMessage("Não é possível " + action + " um objeto na situação " + from.label() + ".");
    }

    @Test
    void coversEveryCombinationOfStatusAndAction() {
        long combinations = (long) FoundItemStatus.ALL.size() * ACTIONS.size();

        assertThat(ALLOWED.size() + forbiddenTransitions().count()).isEqualTo(combinations);
        assertThat(forbiddenTransitions().count()).isEqualTo(17);
    }

    @Test
    void aNewItemStartsAvailable() {
        assertThat(FoundItemStatus.initial()).isEqualTo(AVAILABLE);
    }

    @Test
    void onlyReturnedAndDisposedAreFinal() {
        assertThat(FoundItemStatus.ALL).filteredOn(FoundItemStatus::isFinal).containsExactly(RETURNED, DISPOSED);
    }

    @Test
    void aFinalStatusAllowsNoAction() {
        for (FoundItemStatus status : List.of(RETURNED, DISPOSED)) {
            ACTIONS.values().forEach(action -> assertThatThrownBy(() -> action.apply(status))
                    .isInstanceOf(InvalidStatusTransitionException.class));
        }
    }

    @Test
    void aReservedItemThatWasUnclaimedGoesBackToAvailable() {
        assertThat(UNCLAIMED.reserve().release()).isEqualTo(AVAILABLE);
    }

    @Test
    void theConverterStoresTheCodeAndReadsTheSameStatusBack() {
        FoundItemStatusConverter converter = new FoundItemStatusConverter();

        for (FoundItemStatus status : FoundItemStatus.ALL) {
            String column = converter.convertToDatabaseColumn(status);
            assertThat(column).isEqualTo(status.code()).matches("[A-Z]+");
            assertThat(converter.convertToEntityAttribute(column)).isEqualTo(status);
        }
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void refusesAnUnknownCode() {
        assertThatThrownBy(() -> FoundItemStatus.fromCode("LOST"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LOST");
    }
}
