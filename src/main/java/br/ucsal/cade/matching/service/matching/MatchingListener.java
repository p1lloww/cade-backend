package br.ucsal.cade.matching.service.matching;

import br.ucsal.cade.founditems.api.event.FoundItemRegisteredEvent;
import br.ucsal.cade.lossreports.api.event.LossReportRegisteredEvent;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Dispara o cruzamento quando um objeto é cadastrado ou uma perda é registrada (seção 7.2, Observer). Só depois do
 * commit, para não cruzar um cadastro que foi desfeito, e numa Virtual Thread (D05), para o cadastro responder sem
 * esperar o cálculo.
 *
 * <p>Uma falha aqui fica no log e não chega a quem cadastrou (RNF08): o cadastro já está gravado.
 */
@Component
class MatchingListener {

    private static final Logger log = LoggerFactory.getLogger(MatchingListener.class);

    private final MatchingService matching;

    MatchingListener(MatchingService matching) {
        this.matching = matching;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(FoundItemRegisteredEvent event) {
        run("found item", event.foundItemId(), () -> matching.matchFoundItem(event.foundItemId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(LossReportRegisteredEvent event) {
        run("loss report", event.lossReportId(), () -> matching.matchLossReport(event.lossReportId()));
    }

    private void run(String subject, UUID id, Supplier<List<MatchSuggestion>> match) {
        try {
            List<MatchSuggestion> created = match.get();
            log.info("Matching for {} {} created {} suggestion(s)", subject, id, created.size());
        } catch (RuntimeException e) {
            log.error("Matching for {} {} failed; the registration is kept", subject, id, e);
        }
    }
}
