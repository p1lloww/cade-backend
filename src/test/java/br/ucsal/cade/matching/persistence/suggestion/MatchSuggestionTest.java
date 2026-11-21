package br.ucsal.cade.matching.persistence.suggestion;

import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchSuggestionTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");

    @Test
    void aSuggestionStartsNewWithItsOwnId() {
        UUID item = UUID.randomUUID();
        UUID report = UUID.randomUUID();

        MatchSuggestion suggestion = MatchSuggestion.suggest(item, report, 80, NOW);

        assertThat(suggestion.getId()).isNotNull();
        assertThat(suggestion.getFoundItemId()).isEqualTo(item);
        assertThat(suggestion.getLossReportId()).isEqualTo(report);
        assertThat(suggestion.getScore()).isEqualTo(80);
        assertThat(suggestion.getStatus()).isEqualTo(SuggestionStatus.NEW);
        assertThat(suggestion.getCreatedAt()).isEqualTo(NOW);
        assertThat(MatchSuggestion.suggest(item, report, 80, NOW).getId()).isNotEqualTo(suggestion.getId());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    void refusesAScoreOutsideZeroToOneHundred(int score) {
        assertThatThrownBy(() -> MatchSuggestion.suggest(UUID.randomUUID(), UUID.randomUUID(), score, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void discardingANewSuggestionMakesItDiscarded() {
        MatchSuggestion suggestion = MatchSuggestion.suggest(UUID.randomUUID(), UUID.randomUUID(), 80, NOW);

        suggestion.discard();

        assertThat(suggestion.getStatus()).isEqualTo(SuggestionStatus.DISCARDED);
    }

    @Test
    void aDiscardedSuggestionCannotBeDiscardedAgain() {
        MatchSuggestion suggestion = MatchSuggestion.suggest(UUID.randomUUID(), UUID.randomUUID(), 80, NOW);
        suggestion.discard();

        assertThatThrownBy(suggestion::discard).isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(suggestion.getStatus()).isEqualTo(SuggestionStatus.DISCARDED);
    }
}
