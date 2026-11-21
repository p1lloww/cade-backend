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
import java.util.function.Consumer;

@Service
class LossReportSummonsService implements LossReportSummons {

    static final String SUMMONED_ACTION = "LOSS_REPORT_SUMMONED";
    static final String SUMMONED_REASON = "Estudante convocado para retirar o objeto.";
    static final String COMPLETED_ACTION = "LOSS_REPORT_COMPLETED";
    static final String COMPLETED_REASON = "Objeto retirado pelo estudante no balcão.";
    static final String RELEASED_ACTION = "LOSS_REPORT_RELEASED";
    static final String REFUSED_ACTION = "LOSS_REPORT_REFUSED";

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
        LossReport report = load(lossReportId, "Summons");
        change(report, LossReport::summon, "Summons");
        audit(staffId, SUMMONED_ACTION, report, SUMMONED_REASON);
        log.debug("Loss report {} summoned by {}", lossReportId, staffId);
        return new SummonedLossReport(report.getId(), report.getStudentId());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(UUID lossReportId, UUID staffId) {
        LossReport report = load(lossReportId, "Completion");
        change(report, LossReport::complete, "Completion");
        audit(staffId, COMPLETED_ACTION, report, COMPLETED_REASON);
        log.debug("Loss report {} completed by {}", lossReportId, staffId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void refuse(UUID lossReportId, UUID staffId, String reason) {
        LossReport report = load(lossReportId, "Refusal");
        change(report, LossReport::refuse, "Refusal");
        audit(staffId, REFUSED_ACTION, report, reason);
        log.debug("Loss report {} refused by {}", lossReportId, staffId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void release(UUID lossReportId, UUID actorId, String reason) {
        LossReport report = load(lossReportId, "Release");
        change(report, LossReport::release, "Release");
        audit(actorId, RELEASED_ACTION, report, reason);
        log.debug("Loss report {} released by {}", lossReportId, actorId);
    }

    private LossReport load(UUID lossReportId, String operation) {
        return reports.findById(lossReportId).orElseThrow(() -> {
            log.info("{} refused: loss report {} not found", operation, lossReportId);
            return new LossReportNotFoundException();
        });
    }

    private static void change(LossReport report, Consumer<LossReport> transition, String operation) {
        try {
            transition.accept(report);
        } catch (InvalidStatusTransitionException e) {
            log.info("{} refused for loss report {}: status is {}", operation, report.getId(),
                    report.getStatus().code());
            throw e;
        }
    }

    private void audit(UUID actorId, String action, LossReport report, String reason) {
        events.publish(new AuditEvent(actorId, action, LossReport.class.getSimpleName(), report.getId(), reason));
    }
}
