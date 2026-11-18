package br.ucsal.cade.administration.persistence.audit;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.shared.audit.AuditEvent;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AuditRecordRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-13T12:00:00Z");

    @Autowired
    private AuditRecordRepository records;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndReadsARecordWithAllFieldsAndGeneratedId() {
        UUID actorId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        AuditEvent event = new AuditEvent(actorId, "FOUND_ITEM_REGISTERED", "FoundItem", entityId,
                "Objeto cadastrado pelo funcionário");

        AuditRecord saved = records.save(AuditRecord.of(event, NOW));
        entityManager.flush();
        entityManager.clear();

        AuditRecord found = records.findById(saved.getId()).orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(found.getActorId()).isEqualTo(actorId);
        assertThat(found.getAction()).isEqualTo("FOUND_ITEM_REGISTERED");
        assertThat(found.getEntityType()).isEqualTo("FoundItem");
        assertThat(found.getEntityId()).isEqualTo(entityId);
        assertThat(found.getReason()).isEqualTo("Objeto cadastrado pelo funcionário");
        assertThat(found.getOccurredAt()).isEqualTo(NOW);
    }

    @Test
    void doesNotExposeDeleteOrRemoveMethods() {
        assertThat(AuditRecordRepository.class.getMethods())
                .extracting(Method::getName)
                .noneMatch(name -> name.startsWith("delete") || name.startsWith("remove"));
    }
}
