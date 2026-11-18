package br.ucsal.cade.administration.service.audit;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.administration.persistence.audit.AuditRecord;
import br.ucsal.cade.administration.persistence.audit.AuditRecordRepository;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class AuditRecorderTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AuditRecordRepository records;

    @Autowired
    private MutableClock clock;

    @Test
    void savesExactlyOneRecordWhenTheTransactionCommits() {
        UUID entityId = UUID.randomUUID();
        AuditEvent event = eventFor(entityId);
        Instant expectedTime = clock.instant();

        transactionTemplate.executeWithoutResult(status -> publisher.publishEvent(event));

        List<AuditRecord> found = recordsFor(entityId);
        assertThat(found).hasSize(1);
        AuditRecord record = found.get(0);
        assertThat(record.getId()).isNotNull();
        assertThat(record.getActorId()).isEqualTo(event.actorId());
        assertThat(record.getAction()).isEqualTo(event.action());
        assertThat(record.getEntityType()).isEqualTo(event.entityType());
        assertThat(record.getEntityId()).isEqualTo(entityId);
        assertThat(record.getReason()).isEqualTo(event.reason());
        assertThat(record.getOccurredAt()).isEqualTo(expectedTime);
    }

    @Test
    void leavesNoRecordWhenTheTransactionRollsBack() {
        UUID entityId = UUID.randomUUID();
        AuditEvent event = eventFor(entityId);

        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishEvent(event);
            status.setRollbackOnly();
        });

        assertThat(recordsFor(entityId)).isEmpty();
    }

    @Test
    void failsWhenTheEventIsPublishedWithoutATransaction() {
        UUID entityId = UUID.randomUUID();
        AuditEvent event = eventFor(entityId);

        assertThatThrownBy(() -> publisher.publishEvent(event))
                .isInstanceOf(IllegalTransactionStateException.class);

        assertThat(recordsFor(entityId)).isEmpty();
    }

    private AuditEvent eventFor(UUID entityId) {
        return new AuditEvent(UUID.randomUUID(), "FOUND_ITEM_REGISTERED", "FoundItem", entityId,
                "Objeto cadastrado pelo funcionário");
    }

    private List<AuditRecord> recordsFor(UUID entityId) {
        return records.findAll().stream()
                .filter(record -> record.getEntityId().equals(entityId))
                .toList();
    }
}
