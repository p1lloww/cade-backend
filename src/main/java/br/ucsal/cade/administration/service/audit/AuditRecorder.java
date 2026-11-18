package br.ucsal.cade.administration.service.audit;

import br.ucsal.cade.administration.persistence.audit.AuditRecord;
import br.ucsal.cade.administration.persistence.audit.AuditRecordRepository;
import br.ucsal.cade.shared.audit.AuditEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Grava no histórico cada {@link AuditEvent} publicado pelos módulos (D10).
 * <p>
 * O ouvinte é síncrono (sem {@code @Async} e sem {@code AFTER_COMMIT}) e exige uma transação já aberta
 * ({@code MANDATORY}). Assim o registro entra na mesma transação da ação: se uma falhar, a outra é desfeita. Se
 * alguém publicar o evento sem transação, ocorre {@code IllegalTransactionStateException}.
 */
@Component
public class AuditRecorder {

    private final AuditRecordRepository records;
    private final Clock clock;

    public AuditRecorder(AuditRecordRepository records, Clock clock) {
        this.records = records;
        this.clock = clock;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditEvent event) {
        records.save(AuditRecord.of(event, clock.instant()));
    }
}
