package br.ucsal.cade.administration.persistence.audit;

import br.ucsal.cade.shared.audit.AuditEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro do histórico de ações (RF43). Nunca é alterado nem apagado (RN19): a entidade não tem setters, as colunas
 * não aceitam update e o Hibernate trata a classe como imutável.
 * <p>
 * Isso protege o código da aplicação; não impede um DELETE feito direto no banco nem um
 * {@code EntityManager.remove}.
 */
@Entity
@Immutable
@Table(name = "audit_records")
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID actorId;

    @Column(nullable = false, updatable = false, length = AuditEvent.MAX_NAME_LENGTH)
    private String action;

    @Column(nullable = false, updatable = false, length = AuditEvent.MAX_NAME_LENGTH)
    private String entityType;

    @Column(nullable = false, updatable = false)
    private UUID entityId;

    @Column(nullable = false, updatable = false, length = AuditEvent.MAX_REASON_LENGTH)
    private String reason;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditRecord() {
    }

    private AuditRecord(AuditEvent event, Instant occurredAt) {
        Objects.requireNonNull(event);
        this.actorId = event.actorId();
        this.action = event.action();
        this.entityType = event.entityType();
        this.entityId = event.entityId();
        this.reason = event.reason();
        this.occurredAt = Objects.requireNonNull(occurredAt);
    }

    /** Cria o registro a partir do evento publicado, com a hora informada por quem grava. */
    public static AuditRecord of(AuditEvent event, Instant occurredAt) {
        return new AuditRecord(event, occurredAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
