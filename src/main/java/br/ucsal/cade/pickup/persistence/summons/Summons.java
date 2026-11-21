package br.ucsal.cade.pickup.persistence.summons;

import br.ucsal.cade.pickup.persistence.status.SummonsStatus;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.category.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Convocação: o funcionário chamou o estudante para retirar o objeto a partir de uma sugestão (RF26). O objeto fica
 * reservado até a retirada, a recusa no balcão ou o fim do prazo (RN15). Retirada e recusa disputam a linha pelo
 * {@code @Version}: das duas, só uma é gravada.
 *
 * <p>Sugestão, objeto, perda e estudante são referências por id, sem chave estrangeira: as tabelas são de outros
 * módulos (D10). Uma sugestão é convocada no máximo uma vez.
 *
 * <p>Guarda o risco do objeto no momento da convocação, que define o que o estudante leva ao balcão (RN09). Ele não
 * muda depois, porque o risco de um objeto Reservado não pode ser alterado (complemento da D21).
 */
@Entity
@Table(name = "summons")
public class Summons {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false, unique = true)
    private UUID suggestionId;

    @Column(nullable = false, updatable = false)
    private UUID foundItemId;

    @Column(nullable = false, updatable = false)
    private UUID lossReportId;

    @Column(nullable = false, updatable = false)
    private UUID studentId;

    @Column(nullable = false, updatable = false)
    private UUID summonedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private RiskLevel risk;

    @Column(nullable = false, length = 20)
    private SummonsStatus status;

    @Column(nullable = false, updatable = false)
    private Instant summonedAt;

    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    /** Quem registrou a retirada ou a recusa; nulo enquanto Ativa ou depois de vencer. */
    private UUID closedBy;

    private Instant closedAt;

    /** Por que a verificação falhou no balcão (RF31); só nas recusadas. */
    @Column(length = AuditEvent.MAX_REASON_LENGTH)
    private String refusalReason;

    @Version
    private long version;

    protected Summons() {
    }

    private Summons(NewSummons data, Instant summonedAt, Instant expiresAt) {
        if (!expiresAt.isAfter(summonedAt)) {
            throw new IllegalArgumentException("O prazo da convocação precisa terminar depois dela.");
        }
        this.suggestionId = Objects.requireNonNull(data.suggestionId());
        this.foundItemId = Objects.requireNonNull(data.foundItemId());
        this.lossReportId = Objects.requireNonNull(data.lossReportId());
        this.studentId = Objects.requireNonNull(data.studentId());
        this.summonedBy = Objects.requireNonNull(data.summonedBy());
        this.risk = Objects.requireNonNull(data.risk());
        this.status = SummonsStatus.initial();
        this.summonedAt = summonedAt;
        this.expiresAt = expiresAt;
    }

    /** Uma convocação Ativa, que vence em {@code expiresAt} se o estudante não retirar o objeto (RN15). */
    public static Summons issue(NewSummons data, Instant summonedAt, Instant expiresAt) {
        return new Summons(data, Objects.requireNonNull(summonedAt), Objects.requireNonNull(expiresAt));
    }

    /** Ativa → Concluída: o estudante retirou o objeto no balcão (RF29). */
    public void complete(UUID staffId, Instant at) {
        status = status.complete();
        close(staffId, at);
    }

    /** Ativa → Recusada: a verificação falhou no balcão (RF31); conta para o limite de recusas (RN12). */
    public void refuse(UUID staffId, Instant at, String reason) {
        status = status.refuse();
        close(staffId, at);
        refusalReason = reason;
    }

    private void close(UUID staffId, Instant at) {
        closedBy = Objects.requireNonNull(staffId);
        closedAt = Objects.requireNonNull(at);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSuggestionId() {
        return suggestionId;
    }

    public UUID getFoundItemId() {
        return foundItemId;
    }

    public UUID getLossReportId() {
        return lossReportId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getSummonedBy() {
        return summonedBy;
    }

    public RiskLevel getRisk() {
        return risk;
    }

    public SummonsStatus getStatus() {
        return status;
    }

    public Instant getSummonedAt() {
        return summonedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public UUID getClosedBy() {
        return closedBy;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public String getRefusalReason() {
        return refusalReason;
    }

    public long getVersion() {
        return version;
    }
}
