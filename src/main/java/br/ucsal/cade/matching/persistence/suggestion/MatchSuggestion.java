package br.ucsal.cade.matching.persistence.suggestion;

import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
}
