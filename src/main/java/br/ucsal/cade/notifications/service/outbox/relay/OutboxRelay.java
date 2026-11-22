package br.ucsal.cade.notifications.service.outbox.relay;

import br.ucsal.cade.notifications.persistence.outbox.NotificationRepository;
import br.ucsal.cade.notifications.service.outbox.config.OutboxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * O relay da caixa de saída (D30): tira os avisos do banco e os entrega. Duas portas de saída, que usam a mesma
 * {@link NotificationDelivery}:
 *
 * <ul>
 *   <li>{@link #deliverSoon}, logo depois do commit da ação, numa Virtual Thread: o caminho normal, sem esperar;</li>
 *   <li>{@link #relayPending}, de tempos em tempos: reenvia o que falhou e o que ficou para trás numa queda da
 *       aplicação entre o commit e o envio (RNF08).</li>
 * </ul>
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final NotificationRepository notifications;
    private final NotificationDelivery delivery;
    private final OutboxProperties properties;
    private final Clock clock;

    OutboxRelay(NotificationRepository notifications, NotificationDelivery delivery, OutboxProperties properties,
                Clock clock) {
        this.notifications = notifications;
        this.delivery = delivery;
        this.properties = properties;
        this.clock = clock;
    }

    @Async
    public void deliverSoon(UUID notificationId) {
        delivery.deliver(notificationId);
    }

    /** Cada aviso é uma transação: a falha de um não desfaz os outros. */
    @Scheduled(fixedDelayString = "${cade.notifications.relay-interval}",
            initialDelayString = "${cade.notifications.relay-interval}")
    public void relayPending() {
        List<UUID> due = notifications.findDueIds(clock.instant(), properties.batchSize());
        if (due.isEmpty()) {
            return;
        }
        log.info("Relaying {} pending notification(s)", due.size());
        for (UUID id : due) {
            try {
                delivery.deliver(id);
            } catch (RuntimeException e) {
                log.error("Notification {} could not be relayed; it stays pending", id, e);
            }
        }
    }
}
