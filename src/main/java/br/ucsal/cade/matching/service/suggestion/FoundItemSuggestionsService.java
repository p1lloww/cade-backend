package br.ucsal.cade.matching.service.suggestion;

import br.ucsal.cade.matching.api.FoundItemSuggestions;
import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
class FoundItemSuggestionsService implements FoundItemSuggestions {

    static final String CLOSED_ACTION = SuggestionSummonsService.CLOSED_ACTION;
    static final String CLOSED_REASON = "O objeto foi retirado no balcão sem convocação.";

    private static final List<SuggestionStatus> OPEN = List.of(SuggestionStatus.NEW, SuggestionStatus.ON_HOLD);

    private static final Logger log = LoggerFactory.getLogger(FoundItemSuggestionsService.class);

    private final MatchSuggestionRepository suggestions;
    private final EventPublisher events;

    FoundItemSuggestionsService(MatchSuggestionRepository suggestions, EventPublisher events) {
        this.suggestions = suggestions;
        this.events = events;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<UUID> closeOpenOf(UUID foundItemId, UUID staffId) {
        List<MatchSuggestion> open = suggestions.findByFoundItemIdAndStatusInOrderByScoreDescCreatedAtAsc(
                foundItemId, OPEN);
        open.forEach(suggestion -> {
            suggestion.close();
            events.publish(new AuditEvent(staffId, CLOSED_ACTION, MatchSuggestion.class.getSimpleName(),
                    suggestion.getId(), CLOSED_REASON));
        });
        log.debug("{} open suggestion(s) of found item {} closed by {}", open.size(), foundItemId, staffId);
        return open.stream().map(MatchSuggestion::getId).toList();
    }
}
