package br.ucsal.cade.notifications.service.pickup;

import br.ucsal.cade.notifications.persistence.outbox.NotificationKind;
import br.ucsal.cade.notifications.service.outbox.enqueue.Outbox;
import br.ucsal.cade.pickup.api.event.PickupRegisteredEvent;
import br.ucsal.cade.pickup.api.event.SummonsCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Põe na caixa de saída os e-mails da convocação e da retirada (RF34, D30).
 *
 * <p>O ouvinte é síncrono e exige a transação de quem publicou ({@code MANDATORY}), como o histórico: o aviso é
 * gravado junto com a convocação ou a retirada, e some se ela for desfeita. O envio fica para depois do commit, no
 * relay da caixa de saída. Aqui só se monta o texto e se grava a linha; nada que dependa de rede.
 */
@Component
class PickupNoticeListener {

    private static final Logger log = LoggerFactory.getLogger(PickupNoticeListener.class);

    private final Outbox outbox;
    private final Clock clock;

    PickupNoticeListener(Outbox outbox, Clock clock) {
        this.outbox = outbox;
        this.clock = clock;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(SummonsCreatedEvent event) {
        outbox.enqueue(NotificationKind.SUMMONS_CREATED, event.summonsId(), event.studentId(),
                SummonsCreatedNotice.from(event.instructions()));
        log.debug("Summons e-mail queued: summons={}, student={}", event.summonsId(), event.studentId());
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(PickupRegisteredEvent event) {
        outbox.enqueue(NotificationKind.PICKUP_REGISTERED, event.pickupId(), event.studentId(),
                PickupRegisteredNotice.at(clock.instant()));
        log.debug("Pickup e-mail queued: pickup={}, student={}", event.pickupId(), event.studentId());
    }
}
