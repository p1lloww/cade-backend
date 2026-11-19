package br.ucsal.cade.lossreports.persistence.report;

import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface LossReportRepository extends JpaRepository<LossReport, UUID> {

    /** Registro de outro estudante fica invisível (RF14). */
    Optional<LossReport> findByIdAndStudentId(UUID id, UUID studentId);

    /** RN05. */
    long countByStudentIdAndStatusIn(UUID studentId, Collection<LossReportStatus> statuses);
}
