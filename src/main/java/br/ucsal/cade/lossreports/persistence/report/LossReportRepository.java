package br.ucsal.cade.lossreports.persistence.report;

import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface LossReportRepository extends JpaRepository<LossReport, UUID> {

    /** O registro, só se for do estudante; o de outro estudante fica invisível (RF14). */
    Optional<LossReport> findByIdAndStudentId(UUID id, UUID studentId);

    /** Registros do estudante nas situações informadas; com {@link LossReportStatus#ACTIVE}, o limite da RN05. */
    long countByStudentIdAndStatusIn(UUID studentId, Collection<LossReportStatus> statuses);
}
