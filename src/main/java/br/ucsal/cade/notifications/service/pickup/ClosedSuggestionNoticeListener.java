package br.ucsal.cade.notifications.service.pickup;

import br.ucsal.cade.lossreports.api.LossReportOwners;
import br.ucsal.cade.notifications.persistence.outbox.NotificationKind;
import br.ucsal.cade.notifications.service.outbox.enqueue.Outbox;
import br.ucsal.cade.pickup.api.event.OtherSuggestionsClosedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Avisa os estudantes cujas sugestões foram encerradas porque o objeto foi entregue (RF33, D35). Síncrono e
 * {@code MANDATORY}, como os outros ouvintes: o aviso é gravado junto com a retirada.
 *
 * <p>Cada estudante recebe no máximo um aviso por retirada, mesmo que tenha várias sugestões do mesmo objeto, e o texto
 * é neutro (RN03). Pode ser desligado com {@code cade.notifications.closed-suggestion-notice-enabled=false}.
 */
@Component
class ClosedSuggestionNoticeListener {

    private static final Logger log = LoggerFactory.getLogger(ClosedSuggestionNoticeListener.class);

    private final Outbox outbox;
    private final LossReportOwners owners;
    private final boolean enabled;

    ClosedSuggestionNoticeListener(Outbox outbox, LossReportOwners owners,
                                   @Value("${cade.notifications.closed-suggestion-notice-enabled:true}")
                                   boolean enabled) {
        this.outbox = outbox;
        this.owners = owners;
        this.enabled = enabled;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(OtherSuggestionsClosedEvent event) {
        if (!enabled) {
            log.debug("Closed suggestion notices are off; {} suggestion(s) closed silently", event.closed().size());
            return;
        }
        Map<UUID, UUID> studentOf = owners.studentsOf(event.closed().stream()
                .map(OtherSuggestionsClosedEvent.Closed::lossReportId).collect(Collectors.toSet()));
        Set<UUID> notified = new java.util.HashSet<>();
        for (OtherSuggestionsClosedEvent.Closed closed : event.closed()) {
            UUID student = studentOf.get(closed.lossReportId());
            if (student != null && notified.add(student)) {
                outbox.enqueue(NotificationKind.SUGGESTION_CLOSED, closed.suggestionId(), student,
                        SuggestionClosedNotice.create());
            }
        }
        log.debug("Closed suggestion e-mails queued: item={}, students={}", event.foundItemId(), notified.size());
    }
}
