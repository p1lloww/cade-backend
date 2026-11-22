package br.ucsal.cade.lossreports.persistence.report;

import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LossReportRepository extends JpaRepository<LossReport, UUID> {

    Optional<LossReport> findByIdAndStudentId(UUID id, UUID studentId);

    long countByStudentIdAndStatusIn(UUID studentId, Collection<LossReportStatus> statuses);

    @EntityGraph(attributePaths = "locations")
    Optional<LossReport> findWithLocationsByIdAndStatusIn(UUID id, Collection<LossReportStatus> statuses);

    @EntityGraph(attributePaths = "locations")
    List<LossReport> findWithLocationsByCategoryIdAndStatusIn(UUID categoryId, Collection<LossReportStatus> statuses);

    @EntityGraph(attributePaths = {"category", "locations"})
    List<LossReport> findWithCategoryAndLocationsByIdIn(Collection<UUID> ids);

    @EntityGraph(attributePaths = {"category", "locations"})
    List<LossReport> findWithCategoryAndLocationsByStudentIdOrderByRegisteredAtDesc(UUID studentId);

    List<LossReport> findByStatusInAndExpiresAtGreaterThanAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
            Collection<LossReportStatus> statuses, Instant from, Instant to);
}
