package br.ucsal.cade.matching.persistence.suggestion;

import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Sugestão do cruzamento: um par objeto × perda com compatibilidade média ou alta (RF20, seção 6.3). É apoio ao
 * funcionário, que decide se convoca (RN08).
 *
 * <p>Objeto e perda são referências por id, sem chave estrangeira: as tabelas são de outros módulos (D10). Cada par
 * tem no máximo uma sugestão.
 *
 * <p>Guarda só a nota; a faixa (alta, média) sai dela pelas faixas de {@code cade.matching.bands}.
 */
@Entity
@Table(name = "match_suggestions",
        uniqueConstraints = @UniqueConstraint(name = "uk_match_suggestions_pair",
                columnNames = {"found_item_id", "loss_report_id"}))
public class MatchSuggestion {

    public static final int MAX_SCORE = 100;

    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID foundItemId;

    @Column(nullable = false, updatable = false)
    private UUID lossReportId;

    @Column(nullable = false, updatable = false)
    private int score;

    @Column(nullable = false, length = 20)
    private SuggestionStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Duas decisões ao mesmo tempo sobre a mesma sugestão (descartar e convocar): só a primeira vale. */
    @Version
    private long version;

    protected MatchSuggestion() {
    }

    private MatchSuggestion(UUID foundItemId, UUID lossReportId, int score, Instant createdAt) {
        if (score < 0 || score > MAX_SCORE) {
            throw new IllegalArgumentException("Nota fora de 0 a 100: " + score);
        }
        this.id = UUID.randomUUID();
        this.foundItemId = Objects.requireNonNull(foundItemId);
        this.lossReportId = Objects.requireNonNull(lossReportId);
        this.score = score;
        this.status = SuggestionStatus.initial();
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /**
     * Uma sugestão Nova. O id é gerado aqui, e não pelo banco, porque a gravação é um {@code INSERT} que ignora o par
     * repetido ({@link MatchSuggestionRepository#saveIfAbsent}).
     */
    public static MatchSuggestion suggest(UUID foundItemId, UUID lossReportId, int score, Instant createdAt) {
        return new MatchSuggestion(foundItemId, lossReportId, score, createdAt);
    }

    /** Nova → Descartada. O motivo vai para o histórico, não para a sugestão (RN08). */
    public void discard() {
        status = status.discard();
    }

    /** Nova → Convocada: o funcionário convocou o estudante a partir desta sugestão (RF26). */
    public void summon() {
        status = status.summon();
    }

    /** Nova → Em espera: outra sugestão do mesmo objeto foi convocada (RN18). */
    public void putOnHold() {
        status = status.putOnHold();
    }

    /** Em espera → Nova: a convocação do mesmo objeto foi recusada ou venceu (RN18). */
    public void resume() {
        status = status.resume();
    }

    /** Convocada → Concluída: o estudante retirou o objeto (RF29). */
    public void complete() {
        status = status.complete();
    }

    /** Convocada → Recusada: a verificação falhou no balcão (RF31). */
    public void refuse() {
        status = status.refuse();
    }

    /** Convocada → Vencida: o estudante não retirou o objeto no prazo (RF27). */
    public void expire() {
        status = status.expire();
    }

    /** Nova ou Em espera → Encerrada: a perda ou o objeto desta sugestão não está mais em jogo. */
    public void close() {
        status = status.close();
    }

    public UUID getId() {
        return id;
    }

    public UUID getFoundItemId() {
        return foundItemId;
    }

    public UUID getLossReportId() {
        return lossReportId;
    }

    public int getScore() {
        return score;
    }

    public SuggestionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }
}
