package br.ucsal.cade.matching.service.suggestion;

import br.ucsal.cade.matching.api.RefusedSuggestion;
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
import java.util.function.Consumer;

@Service
class SuggestionSummonsService implements SuggestionSummons {

    static final String SUMMONED_ACTION = "MATCH_SUMMONED";
    static final String SUMMONED_REASON = "Estudante convocado a partir desta sugestão.";
    static final String ON_HOLD_ACTION = "MATCH_ON_HOLD";
    static final String ON_HOLD_REASON = "Outra sugestão do mesmo objeto foi convocada (RN18).";
    static final String COMPLETED_ACTION = "MATCH_COMPLETED";
    static final String COMPLETED_REASON = "Objeto retirado pelo estudante desta sugestão.";
    static final String REFUSED_ACTION = "MATCH_REFUSED";
    static final String REFUSED_REASON = "A verificação falhou no balcão.";
    static final String RESUMED_ACTION = "MATCH_RESUMED";
    static final String RESUMED_REASON = "A convocação do mesmo objeto foi recusada; a sugestão volta a valer (RN18).";
    static final String CLOSED_ACTION = "MATCH_CLOSED";
    static final String CLOSED_REASON = "A perda foi encerrada pela recusa no balcão.";

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

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(UUID suggestionId, UUID staffId) {
        MatchSuggestion suggestion = load(suggestionId);
        change(suggestion, MatchSuggestion::complete, "Completion");
        audit(staffId, COMPLETED_ACTION, suggestion, COMPLETED_REASON);
        log.debug("Suggestion {} completed by {}", suggestionId, staffId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public RefusedSuggestion refuse(UUID suggestionId, UUID staffId) {
        MatchSuggestion suggestion = load(suggestionId);
        change(suggestion, MatchSuggestion::refuse, "Refusal");
        audit(staffId, REFUSED_ACTION, suggestion, REFUSED_REASON);

        List<MatchSuggestion> onHold = suggestions.findByFoundItemIdAndStatusAndIdNot(suggestion.getFoundItemId(),
                SuggestionStatus.ON_HOLD, suggestion.getId());
        onHold.forEach(other -> {
            other.resume();
            audit(staffId, RESUMED_ACTION, other, RESUMED_REASON);
        });

        List<MatchSuggestion> ofTheReport = suggestions.findByLossReportIdAndStatusInAndIdNot(
                suggestion.getLossReportId(), List.of(SuggestionStatus.NEW, SuggestionStatus.ON_HOLD),
                suggestion.getId());
        ofTheReport.forEach(other -> {
            other.close();
            audit(staffId, CLOSED_ACTION, other, CLOSED_REASON);
        });
        log.debug("Suggestion {} refused by {}; {} suggestion(s) of the item resumed, {} of the report closed",
                suggestionId, staffId, onHold.size(), ofTheReport.size());
        return new RefusedSuggestion(ids(onHold), ids(ofTheReport));
    }

    private static List<UUID> ids(List<MatchSuggestion> suggestions) {
        return suggestions.stream().map(MatchSuggestion::getId).toList();
    }

    private static void change(MatchSuggestion suggestion, Consumer<MatchSuggestion> transition, String operation) {
        try {
            transition.accept(suggestion);
        } catch (InvalidStatusTransitionException e) {
            log.info("{} refused for suggestion {}: status is {}", operation, suggestion.getId(),
                    suggestion.getStatus().code());
            throw e;
        }
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
