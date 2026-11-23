package br.ucsal.cade.matching.service.suggestion;

import br.ucsal.cade.lossreports.api.event.LossReportClosedEvent;
import br.ucsal.cade.lossreports.api.event.LossReportInfoOverdueEvent;
import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Acompanha o fim do registro de perda: quando ele é cancelado ou vence (PER-06), as sugestões abertas dele deixam de
 * fazer sentido e são encerradas; quando um pedido de informações fica 5 dias sem resposta, a sugestão que o originou é
 * descartada (RN13).
 *
 * <p>Os ouvintes são síncronos e exigem a transação de quem publicou ({@code MANDATORY}), como o histórico: sugestão e
 * registro mudam juntos ou não mudam. A sugestão é encerrada em nome do sistema (RF43).
 */
@Component
class LossReportEventsListener {

    static final String CLOSED_ACTION = "MATCH_CLOSED";
    static final String CANCELLED_REASON = "O estudante cancelou o registro de perda.";
    static final String EXPIRED_REASON = "O registro de perda venceu sem renovação.";
    static final String DISCARDED_ACTION = "MATCH_DISCARDED";
    static final String OVERDUE_REASON = "Passaram 5 dias sem resposta ao pedido de informações (RN13).";

    private static final Logger log = LoggerFactory.getLogger(LossReportEventsListener.class);

    private final MatchSuggestionRepository suggestions;
    private final EventPublisher events;

    LossReportEventsListener(MatchSuggestionRepository suggestions, EventPublisher events) {
        this.suggestions = suggestions;
        this.events = events;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(LossReportClosedEvent event) {
        String reason = event.reason() == LossReportClosedEvent.Reason.CANCELLED ? CANCELLED_REASON : EXPIRED_REASON;
        List<MatchSuggestion> open = suggestions.findByLossReportIdAndStatusInOrderByScoreDescCreatedAtAsc(
                event.lossReportId(), List.of(SuggestionStatus.NEW, SuggestionStatus.ON_HOLD));
        open.forEach(suggestion -> {
            suggestion.close();
            audit(CLOSED_ACTION, suggestion, reason);
        });
        log.debug("Loss report {} closed ({}): {} suggestion(s) closed", event.lossReportId(), event.reason(),
                open.size());
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(LossReportInfoOverdueEvent event) {
        suggestions.findById(event.suggestionId())
                .filter(suggestion -> suggestion.getStatus() == SuggestionStatus.NEW)
                .ifPresentOrElse(suggestion -> {
                    suggestion.discard();
                    audit(DISCARDED_ACTION, suggestion, OVERDUE_REASON);
                    log.info("Suggestion {} discarded: no answer to the info request of report {}",
                            suggestion.getId(), event.lossReportId());
                }, () -> log.debug("Suggestion {} is not New anymore; nothing to discard", event.suggestionId()));
    }

    private void audit(String action, MatchSuggestion suggestion, String reason) {
        events.publish(new AuditEvent(AuditEvent.SYSTEM_ACTOR, action, MatchSuggestion.class.getSimpleName(),
                suggestion.getId(), reason));
    }
}
