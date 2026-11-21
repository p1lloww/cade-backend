package br.ucsal.cade.matching.service.suggestion;

import br.ucsal.cade.matching.api.SuggestionPair;
import br.ucsal.cade.matching.api.SuggestionSummons;
import br.ucsal.cade.matching.api.SummonedSuggestion;
import br.ucsal.cade.matching.exception.SuggestionNotFoundException;
import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
class SuggestionSummonsService implements SuggestionSummons {

    static final String SUMMONED_ACTION = "MATCH_SUMMONED";
    static final String SUMMONED_REASON = "Estudante convocado a partir desta sugestão.";
    static final String ON_HOLD_ACTION = "MATCH_ON_HOLD";
    static final String ON_HOLD_REASON = "Outra sugestão do mesmo objeto foi convocada (RN18).";

    private static final Logger log = LoggerFactory.getLogger(SuggestionSummonsService.class);

    private final MatchSuggestionRepository suggestions;
    private final EventPublisher events;

    SuggestionSummonsService(MatchSuggestionRepository suggestions, EventPublisher events) {
        this.suggestions = suggestions;
        this.events = events;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public SuggestionPair pairOf(UUID suggestionId) {
        return pairOf(load(suggestionId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public SummonedSuggestion summon(UUID suggestionId, UUID staffId) {
        MatchSuggestion suggestion = load(suggestionId);
        try {
            suggestion.summon();
        } catch (InvalidStatusTransitionException e) {
            log.info("Summons refused for suggestion {}: status is {}", suggestionId, suggestion.getStatus().code());
            throw e;
        }
        audit(staffId, SUMMONED_ACTION, suggestion, SUMMONED_REASON);

        List<MatchSuggestion> others = suggestions.findByFoundItemIdAndStatusAndIdNot(suggestion.getFoundItemId(),
                SuggestionStatus.NEW, suggestion.getId());
        others.forEach(other -> {
            other.putOnHold();
            audit(staffId, ON_HOLD_ACTION, other, ON_HOLD_REASON);
        });
        log.debug("Suggestion {} summoned by {}; {} other suggestion(s) of the item put on hold", suggestionId,
                staffId, others.size());
        return new SummonedSuggestion(pairOf(suggestion), others.stream().map(MatchSuggestion::getId).toList());
    }

    private void audit(UUID staffId, String action, MatchSuggestion suggestion, String reason) {
        events.publish(new AuditEvent(staffId, action, MatchSuggestion.class.getSimpleName(), suggestion.getId(),
                reason));
    }

    private MatchSuggestion load(UUID suggestionId) {
        return suggestions.findById(suggestionId).orElseThrow(() -> {
            log.info("Suggestion {} not found", suggestionId);
            return new SuggestionNotFoundException();
        });
    }

    private static SuggestionPair pairOf(MatchSuggestion suggestion) {
        return new SuggestionPair(suggestion.getId(), suggestion.getFoundItemId(), suggestion.getLossReportId());
    }
}
