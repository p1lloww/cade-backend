package br.ucsal.cade.lossreports.service.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Tarefa agendada dos prazos do registro de perda: vence os registros com 30 dias sem renovação (RN14) e devolve a
 * Aberto os que passaram 5 dias sem resposta ao pedido de informações (RN13). Roda de
 * {@code cade.loss-reports.expiry-interval} em {@code cade.loss-reports.expiry-interval} (de hora em hora).
 *
 * <p>Não é transacional de propósito: cada registro é tratado na própria transação, então um que falhe (por exemplo, o
 * estudante renovou no mesmo instante e o {@code @Version} recusou) é registrado no log, a próxima rodada tenta de novo
 * e os outros não são afetados.
 */
@Component
class LossReportExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(LossReportExpiryJob.class);

    private final LossReportLifecycleService lifecycle;

    LossReportExpiryJob(LossReportLifecycleService lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Scheduled(fixedDelayString = "${cade.loss-reports.expiry-interval:1h}",
            initialDelayString = "${cade.loss-reports.expiry-interval:1h}")
    public void applyDeadlines() {
        int expired = apply(lifecycle.dueToExpire(), lifecycle::expire, "expire");
        int resumed = apply(lifecycle.infoOverdue(), lifecycle::resumeOverdue, "resume without answer");
        if (expired + resumed > 0) {
            log.info("Loss report deadlines round: expired={}, resumedWithoutAnswer={}", expired, resumed);
        }
    }

    private static int apply(List<UUID> ids, Predicate<UUID> action, String operation) {
        int done = 0;
        for (UUID id : ids) {
            try {
                if (action.test(id)) {
                    done++;
                }
            } catch (RuntimeException e) {
                log.warn("Loss report {} could not {} now; the next round tries again", id, operation, e);
            }
        }
        return done;
    }
}
