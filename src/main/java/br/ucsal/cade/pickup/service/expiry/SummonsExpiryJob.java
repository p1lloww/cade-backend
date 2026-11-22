package br.ucsal.cade.pickup.service.expiry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Tarefa agendada do vencimento das convocações (RF27). Roda de {@code cade.pickup.expiry-interval} em
 * {@code cade.pickup.expiry-interval} (de hora em hora por padrão) e vence cada convocação na própria transação, com
 * {@link SummonsExpiryService#expire(UUID)}.
 *
 * <p>Não é transacional de propósito: uma convocação que falha (por exemplo, o balcão registrou a retirada no mesmo
 * instante e o {@code @Version} recusou o vencimento) é registrada no log e a próxima rodada tenta de novo. As outras
 * convocações da rodada não são afetadas.
 */
@Component
class SummonsExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(SummonsExpiryJob.class);

    private final SummonsExpiryService expiry;

    SummonsExpiryJob(SummonsExpiryService expiry) {
        this.expiry = expiry;
    }

    @Scheduled(fixedDelayString = "${cade.pickup.expiry-interval:1h}",
            initialDelayString = "${cade.pickup.expiry-interval:1h}")
    public void expireDueSummonses() {
        List<UUID> due = expiry.dueIds();
        int expired = 0;
        for (UUID summonsId : due) {
            try {
                if (expiry.expire(summonsId)) {
                    expired++;
                }
            } catch (RuntimeException e) {
                log.warn("Summons {} could not be expired now; the next round tries again", summonsId, e);
            }
        }
        if (!due.isEmpty()) {
            log.info("Summons expiry round: due={}, expired={}", due.size(), expired);
        }
    }
}
