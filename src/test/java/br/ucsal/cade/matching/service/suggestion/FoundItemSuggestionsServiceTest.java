package br.ucsal.cade.matching.service.suggestion;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.matching.api.FoundItemSuggestions;
import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** As sugestões apontam para ids soltos de objeto e perda: não há chave estrangeira entre os módulos (D10). */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class FoundItemSuggestionsServiceTest {

    @Autowired
    private FoundItemSuggestions itemSuggestions;

    @Autowired
    private MatchSuggestionRepository suggestions;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

    @Test
    void closesOnlyTheNewAndOnHoldSuggestionsOfTheItemWithHistory() {
        UUID item = UUID.randomUUID();
        UUID staff = UUID.randomUUID();
        MatchSuggestion fresh = suggest(item);
        MatchSuggestion onHold = suggest(item);
        jdbc.update("UPDATE match_suggestions SET status = 'ON_HOLD' WHERE id = ?", onHold.getId());
        MatchSuggestion discarded = suggest(item);
        jdbc.update("UPDATE match_suggestions SET status = 'DISCARDED' WHERE id = ?", discarded.getId());
        MatchSuggestion ofAnotherItem = suggest(UUID.randomUUID());

        List<UUID> closed = transaction.execute(_ -> itemSuggestions.closeOpenOf(item, staff));

        assertThat(closed).containsExactlyInAnyOrder(fresh.getId(), onHold.getId());
        assertThat(status(fresh)).isEqualTo(SuggestionStatus.CLOSED);
        assertThat(status(onHold)).isEqualTo(SuggestionStatus.CLOSED);
        assertThat(status(discarded)).isEqualTo(SuggestionStatus.DISCARDED);
        assertThat(status(ofAnotherItem)).isEqualTo(SuggestionStatus.NEW);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_records WHERE actor_id = ? AND action = ?",
                Long.class, staff, "MATCH_CLOSED")).isEqualTo(2);
    }

    @Test
    void anItemWithoutSuggestionsClosesNothing() {
        List<UUID> closed = transaction.execute(_ -> itemSuggestions.closeOpenOf(UUID.randomUUID(),
                UUID.randomUUID()));

        assertThat(closed).isEmpty();
    }

    private MatchSuggestion suggest(UUID item) {
        return suggestions.saveAndFlush(MatchSuggestion.suggest(item, UUID.randomUUID(), 60, clock.instant()));
    }

    private SuggestionStatus status(MatchSuggestion suggestion) {
        return suggestions.findById(suggestion.getId()).orElseThrow().getStatus();
    }
}
