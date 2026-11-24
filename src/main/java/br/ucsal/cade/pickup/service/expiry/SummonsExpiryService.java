package br.ucsal.cade.pickup.service.expiry;

import br.ucsal.cade.founditems.api.reservation.FoundItemReservations;
import br.ucsal.cade.lossreports.api.summons.LossReportSummons;
import br.ucsal.cade.matching.api.ExpiredSuggestion;
import br.ucsal.cade.matching.api.SuggestionSummons;
import br.ucsal.cade.pickup.api.event.SummonsExpiredEvent;
import br.ucsal.cade.pickup.persistence.status.SummonsStatus;
import br.ucsal.cade.pickup.persistence.summons.Summons;
import br.ucsal.cade.pickup.persistence.summons.SummonsRepository;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * O vencimento da convocação (RF27): sem retirada em 7 dias (RN15), a convocação vence, o objeto volta a Disponível, a
 * perda volta a Aberto e as sugestões Em espera do mesmo objeto voltam a Nova (RN18). Tudo na mesma transação, com cada
 * mudança no histórico em nome do sistema (RF43).
 *
 * <p>Como na retirada e na recusa, a convocação é gravada primeiro: se o funcionário registrar a retirada ou a recusa
 * no mesmo instante, as duas disputam a linha pelo {@code @Version} e só uma é gravada (a que perde não muda nada).
 *
 * <p>Cada convocação vence na sua própria transação: uma que falhe não desfaz o vencimento das outras.
 */
@Service
public class SummonsExpiryService {

    static final String EXPIRED_ACTION = "SUMMONS_EXPIRED";
    static final String EXPIRED_REASON = "O estudante não retirou o objeto dentro do prazo da convocação (RN15).";
    static final String RELEASE_REASON = "A convocação venceu sem retirada; o objeto volta a ficar disponível.";
    static final String REPORT_REASON = "A convocação venceu sem retirada; o registro de perda volta a ficar aberto.";

    private static final Logger log = LoggerFactory.getLogger(SummonsExpiryService.class);

    private final SummonsRepository summonses;
    private final FoundItemReservations foundItems;
    private final LossReportSummons lossReports;
    private final SuggestionSummons suggestions;
    private final EventPublisher events;
    private final Clock clock;

    SummonsExpiryService(SummonsRepository summonses, FoundItemReservations foundItems, LossReportSummons lossReports,
                         SuggestionSummons suggestions, EventPublisher events, Clock clock) {
        this.summonses = summonses;
        this.foundItems = foundItems;
        this.lossReports = lossReports;
        this.suggestions = suggestions;
        this.events = events;
        this.clock = clock;
    }

    /** Os ids das convocações Ativas cujo prazo já passou, das mais antigas para as mais novas. */
    @Transactional(readOnly = true)
    public List<UUID> dueIds() {
        return summonses.findByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(SummonsStatus.ACTIVE,
                clock.instant()).stream().map(Summons::getId).toList();
    }

    /**
     * Vence uma convocação. O estado é conferido de novo aqui dentro da transação, porque o balcão pode ter registrado
     * a retirada ou a recusa depois que a tarefa listou as convocações.
     *
     * @return {@code true} se a convocação venceu agora; {@code false} se ela já não estava Ativa ou o prazo ainda não
     * passou
     */
    @Transactional
    public boolean expire(UUID summonsId) {
        Instant now = clock.instant();
        Summons summons = summonses.findById(summonsId).orElse(null);
        if (summons == null || summons.getStatus() != SummonsStatus.ACTIVE || summons.getExpiresAt().isAfter(now)) {
            log.debug("Summons {} is not due; skipped", summonsId);
            return false;
        }

        summons.expire(now);
        summonses.saveAndFlush(summons);
        foundItems.release(summons.getFoundItemId(), AuditEvent.SYSTEM_ACTOR, RELEASE_REASON);
        ExpiredSuggestion suggestion = suggestions.expire(summons.getSuggestionId(), AuditEvent.SYSTEM_ACTOR);
        lossReports.release(summons.getLossReportId(), AuditEvent.SYSTEM_ACTOR, REPORT_REASON);

        events.publish(new AuditEvent(AuditEvent.SYSTEM_ACTOR, EXPIRED_ACTION, Summons.class.getSimpleName(),
                summons.getId(), EXPIRED_REASON));
        events.publish(new SummonsExpiredEvent(summons.getId(), summons.getStudentId(), summons.getFoundItemId(),
                summons.getLossReportId()));
        log.info("Summons {} expired: item={}, report={}, resumed={}", summons.getId(), summons.getFoundItemId(),
                summons.getLossReportId(), suggestion.resumed().size());
        return true;
    }
}
