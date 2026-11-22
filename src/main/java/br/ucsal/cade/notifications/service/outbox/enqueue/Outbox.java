package br.ucsal.cade.notifications.service.outbox.enqueue;

import br.ucsal.cade.notifications.persistence.outbox.NotificationKind;
import br.ucsal.cade.notifications.persistence.outbox.NotificationRepository;
import br.ucsal.cade.notifications.service.outbox.relay.OutboxRelay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.util.UUID;

/**
 * A entrada da caixa de saída (Transactional Outbox, D30): grava o aviso na transação de quem chama. Se essa transação
 * for desfeita, o aviso some junto; se for gravada, o aviso já está no banco antes de qualquer tentativa de envio, e
 * uma queda da aplicação não o perde.
 *
 * <p>Depois do commit, pede o envio imediato numa Virtual Thread. Se ele falhar, o {@link OutboxRelay} tenta de novo.
 */
@Service
public class Outbox {

    private static final Logger log = LoggerFactory.getLogger(Outbox.class);

    private final NotificationRepository notifications;
    private final OutboxRelay relay;
    private final Clock clock;

    Outbox(NotificationRepository notifications, OutboxRelay relay, Clock clock) {
        this.notifications = notifications;
        this.relay = relay;
        this.clock = clock;
    }

    /**
     * Só roda dentro de uma transação: é ela que garante que aviso e ação são gravados juntos.
     *
     * @param referenceId o que gerou o aviso (a convocação, a retirada, o registro); com o tipo, identifica o aviso
     * @param recipientId a conta que recebe; o e-mail é buscado na hora do envio
     * @return se o aviso foi gravado; {@code false} se o mesmo tipo para a mesma referência já existia
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean enqueue(NotificationKind kind, UUID referenceId, UUID recipientId, NotificationContent content) {
        UUID id = UUID.randomUUID();
        boolean inserted = notifications.insertIfAbsent(id, kind.name(), referenceId, recipientId, content.subject(),
                content.body(), clock.instant()) == 1;
        if (!inserted) {
            log.debug("Notification {} for {} already in the outbox; ignored", kind, referenceId);
            return false;
        }
        log.debug("Notification {} queued: kind={}, reference={}, recipient={}", id, kind, referenceId, recipientId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                relay.deliverSoon(id);
            }
        });
        return true;
    }
}
