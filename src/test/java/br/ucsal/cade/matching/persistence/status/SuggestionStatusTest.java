package br.ucsal.cade.matching.persistence.status;

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

import static br.ucsal.cade.matching.persistence.status.SuggestionStatus.CLOSED;
import static br.ucsal.cade.matching.persistence.status.SuggestionStatus.COMPLETED;
import static br.ucsal.cade.matching.persistence.status.SuggestionStatus.DISCARDED;
import static br.ucsal.cade.matching.persistence.status.SuggestionStatus.EXPIRED;
import static br.ucsal.cade.matching.persistence.status.SuggestionStatus.NEW;
import static br.ucsal.cade.matching.persistence.status.SuggestionStatus.ON_HOLD;
import static br.ucsal.cade.matching.persistence.status.SuggestionStatus.REFUSED;
import static br.ucsal.cade.matching.persistence.status.SuggestionStatus.SUMMONED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** As transições da seção 6.3 dos requisitos (RN01, RNF13): todas as permitidas e todas as proibidas. */
class SuggestionStatusTest {

    private static final Map<String, UnaryOperator<SuggestionStatus>> ACTIONS = Map.of(
            "pôr em espera", SuggestionStatus::putOnHold,
            "retomar", SuggestionStatus::resume,
            "convocar", SuggestionStatus::summon,
            "descartar", SuggestionStatus::discard,
            "concluir", SuggestionStatus::complete,
            "recusar", SuggestionStatus::refuse,
            "vencer", SuggestionStatus::expire,
            "encerrar", SuggestionStatus::close);

    private static final List<Transition> ALLOWED = List.of(
            new Transition(NEW, "pôr em espera", ON_HOLD),
            new Transition(ON_HOLD, "retomar", NEW),
            new Transition(NEW, "convocar", SUMMONED),
            new Transition(NEW, "descartar", DISCARDED),
            new Transition(SUMMONED, "concluir", COMPLETED),
            new Transition(SUMMONED, "recusar", REFUSED),
            new Transition(SUMMONED, "vencer", EXPIRED),
            new Transition(NEW, "encerrar", CLOSED),
            new Transition(ON_HOLD, "encerrar", CLOSED));

    private record Transition(SuggestionStatus from, String action, SuggestionStatus to) {
    }

    static Stream<Arguments> allowedTransitions() {
        return ALLOWED.stream().map(t -> Arguments.of(
                Named.of(t.from().label(), t.from()), Named.of(t.action(), ACTIONS.get(t.action())), t.to()));
    }

    static Stream<Arguments> forbiddenTransitions() {
        return SuggestionStatus.ALL.stream().flatMap(from -> ACTIONS.keySet().stream()
                .filter(action -> ALLOWED.stream().noneMatch(t -> t.from().equals(from) && t.action().equals(action)))
                .map(action -> Arguments.of(Named.of(from.label(), from), action)));
    }

    @ParameterizedTest(name = "{0} → {1} → {2}")
    @MethodSource("allowedTransitions")
    void followsEveryAllowedTransition(SuggestionStatus from, UnaryOperator<SuggestionStatus> action,
                                       SuggestionStatus expected) {
        assertThat(action.apply(from)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} não pode {1}")
    @MethodSource("forbiddenTransitions")
    void refusesEveryOtherTransition(SuggestionStatus from, String action) {
        assertThatThrownBy(() -> ACTIONS.get(action).apply(from))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .isInstanceOf(ConflictException.class)
                .hasMessage("Não é possível " + action + " uma sugestão na situação " + from.label() + ".");
    }

    @Test
    void coversEveryCombinationOfStatusAndAction() {
        long combinations = (long) SuggestionStatus.ALL.size() * ACTIONS.size();

        assertThat(ALLOWED.size() + forbiddenTransitions().count()).isEqualTo(combinations);
        assertThat(forbiddenTransitions().count()).isEqualTo(55);
    }

    @Test
    void aNewSuggestionStartsNew() {
        assertThat(SuggestionStatus.initial()).isEqualTo(NEW);
    }

    @Test
    void finalStatusesAreTheOnesWithoutActions() {
        assertThat(SuggestionStatus.ALL.stream().filter(SuggestionStatus::isFinal))
                .containsExactly(COMPLETED, DISCARDED, REFUSED, EXPIRED, CLOSED);
        assertThat(SuggestionStatus.ALL.stream().filter(SuggestionStatus::isFinal))
                .allMatch(status -> ALLOWED.stream().noneMatch(t -> t.from().equals(status)));
    }

    @Test
    void readsBackEveryCode() {
        assertThat(SuggestionStatus.ALL).allMatch(status -> SuggestionStatus.fromCode(status.code()).equals(status));
        assertThatThrownBy(() -> SuggestionStatus.fromCode("UNKNOWN")).isInstanceOf(IllegalArgumentException.class);
    }
}
