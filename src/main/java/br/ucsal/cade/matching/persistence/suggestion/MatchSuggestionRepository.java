package br.ucsal.cade.matching.persistence.suggestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.ucsal.cade.matching.persistence.status.SuggestionStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchSuggestionRepository extends JpaRepository<MatchSuggestion, UUID> {

    Optional<MatchSuggestion> findByFoundItemIdAndLossReportId(UUID foundItemId, UUID lossReportId);

    /** Da maior nota para a menor; no empate, a mais antiga primeiro (RF22). */
    List<MatchSuggestion> findByFoundItemIdAndStatusInOrderByScoreDescCreatedAtAsc(
            UUID foundItemId, Collection<SuggestionStatus> statuses);

    /** Da maior nota para a menor; no empate, a mais antiga primeiro (RF22). */
    List<MatchSuggestion> findByLossReportIdAndStatusInOrderByScoreDescCreatedAtAsc(
            UUID lossReportId, Collection<SuggestionStatus> statuses);

    /**
     * Grava a sugestão, a menos que o par já tenha uma. Um objeto e uma perda cadastrados no mesmo instante disparam
     * dois cruzamentos que podem achar o mesmo par; com o {@code ON CONFLICT}, o segundo não grava nada e não falha,
     * sem perder as outras sugestões da mesma rodada.
     *
     * @return se a sugestão foi gravada
     */
    default boolean saveIfAbsent(MatchSuggestion suggestion) {
        return insertIfAbsent(suggestion.getId(), suggestion.getFoundItemId(), suggestion.getLossReportId(),
                suggestion.getScore(), suggestion.getStatus().code(), suggestion.getCreatedAt()) == 1;
    }

    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO match_suggestions (id, found_item_id, loss_report_id, score, status, created_at, version)
            VALUES (:id, :foundItemId, :lossReportId, :score, :status, :createdAt, 0)
            ON CONFLICT (found_item_id, loss_report_id) DO NOTHING""")
    int insertIfAbsent(@Param("id") UUID id, @Param("foundItemId") UUID foundItemId,
                       @Param("lossReportId") UUID lossReportId, @Param("score") int score,
                       @Param("status") String status, @Param("createdAt") Instant createdAt);
}
