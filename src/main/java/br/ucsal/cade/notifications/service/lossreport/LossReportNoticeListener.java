package br.ucsal.cade.notifications.service.lossreport;

import br.ucsal.cade.lossreports.api.event.LossReportInfoRequestedEvent;
import br.ucsal.cade.notifications.persistence.outbox.NotificationKind;
import br.ucsal.cade.notifications.service.outbox.enqueue.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Põe na caixa de saída o e-mail do pedido de mais informações (RF35, D30). Síncrono e {@code MANDATORY}, como o
 * ouvinte de retirada: o aviso é gravado junto com o pedido e some se ele for desfeito.
 */
@Component
class LossReportNoticeListener {

    private static final Logger log = LoggerFactory.getLogger(LossReportNoticeListener.class);

    private final Outbox outbox;
    private final Duration responseDeadline;

    LossReportNoticeListener(Outbox outbox,
                             @Value("${cade.loss-reports.info-response-deadline}") Duration responseDeadline) {
        this.outbox = outbox;
        this.responseDeadline = responseDeadline;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(LossReportInfoRequestedEvent event) {
        outbox.enqueue(NotificationKind.INFO_REQUESTED, event.requestId(), event.studentId(),
                InfoRequestedNotice.from(event.reason(), (int) responseDeadline.toDays()));
        log.debug("Info request e-mail queued: report={}, student={}", event.lossReportId(), event.studentId());
    }
}
