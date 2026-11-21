package br.ucsal.cade.lossreports.service.report;

import br.ucsal.cade.lossreports.api.LossReportSummons;
import br.ucsal.cade.lossreports.api.SummonedLossReport;
import br.ucsal.cade.lossreports.persistence.report.LossReport;
import br.ucsal.cade.lossreports.persistence.report.LossReportRepository;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class LossReportSummonsService implements LossReportSummons {

    static final String SUMMONED_ACTION = "LOSS_REPORT_SUMMONED";
    static final String SUMMONED_REASON = "Estudante convocado para retirar o objeto.";

    private static final Logger log = LoggerFactory.getLogger(LossReportSummonsService.class);

    private final LossReportRepository reports;
    private final EventPublisher events;

    LossReportSummonsService(LossReportRepository reports, EventPublisher events) {
        this.reports = reports;
        this.events = events;
    }

    /** Duas convocações simultâneas da mesma perda, com objetos diferentes: o {@code @Version} deixa passar uma. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public SummonedLossReport summon(UUID lossReportId, UUID staffId) {
        LossReport report = reports.findById(lossReportId).orElseThrow(() -> {
            log.info("Summons refused: loss report {} not found", lossReportId);
            return new LossReportNotFoundException();
        });
        try {
            report.summon();
        } catch (InvalidStatusTransitionException e) {
            log.info("Summons refused for loss report {}: status is {}", lossReportId, report.getStatus().code());
            throw e;
        }
        events.publish(new AuditEvent(staffId, SUMMONED_ACTION, LossReport.class.getSimpleName(), report.getId(),
                SUMMONED_REASON));
        log.debug("Loss report {} summoned by {}", lossReportId, staffId);
        return new SummonedLossReport(report.getId(), report.getStudentId());
    }
}
