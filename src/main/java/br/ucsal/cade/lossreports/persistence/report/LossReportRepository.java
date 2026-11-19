package br.ucsal.cade.lossreports.persistence.report;

import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface LossReportRepository extends JpaRepository<LossReport, UUID> {

    /** Registros do estudante nas situações informadas; com {@link LossReportStatus#ACTIVE}, o limite da RN05. */
    long countByStudentIdAndStatusIn(UUID studentId, Collection<LossReportStatus> statuses);
}
