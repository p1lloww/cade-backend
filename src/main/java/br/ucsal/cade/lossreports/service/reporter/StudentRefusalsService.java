package br.ucsal.cade.lossreports.service.reporter;

import br.ucsal.cade.lossreports.api.RecordedRefusal;
import br.ucsal.cade.lossreports.api.StudentRefusals;
import br.ucsal.cade.lossreports.persistence.reporter.LossReporter;
import br.ucsal.cade.lossreports.persistence.reporter.LossReporterRepository;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * As recusas ficam na mesma linha do limite de registros (RN05): o registro de perda já trava essa linha com
 * {@code @Version}, então uma recusa e um registro simultâneos do mesmo estudante não passam os dois com a contagem
 * antiga.
 */
@Service
class StudentRefusalsService implements StudentRefusals {

    static final String REFUSAL_ACTION = "STUDENT_REFUSAL_RECORDED";
    static final String BLOCKED_ACTION = "STUDENT_BLOCKED";

    private static final Logger log = LoggerFactory.getLogger(StudentRefusalsService.class);

    private final LossReporterRepository reporters;
    private final EventPublisher events;

    StudentRefusalsService(LossReporterRepository reporters, EventPublisher events) {
        this.reporters = reporters;
        this.events = events;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public RecordedRefusal record(UUID studentId, UUID staffId) {
        reporters.createIfAbsent(studentId);
        LossReporter reporter = reporters.findLockedByStudentId(studentId).orElseThrow();
        reporter.recordRefusal();
        audit(staffId, REFUSAL_ACTION, studentId, "Recusa no balcão: " + reporter.getRefusals() + " de "
                + LossReporter.MAX_REFUSALS + ".");
        if (reporter.getRefusals() == LossReporter.MAX_REFUSALS) {
            audit(staffId, BLOCKED_ACTION, studentId, "Estudante impedido de abrir novos registros após "
                    + LossReporter.MAX_REFUSALS + " recusas no balcão (RN12).");
            log.info("Student {} blocked after {} refusals", studentId, reporter.getRefusals());
        }
        log.debug("Refusal recorded for student {} by {}: {}", studentId, staffId, reporter.getRefusals());
        return new RecordedRefusal(studentId, reporter.getRefusals(), reporter.isBlocked());
    }

    private void audit(UUID staffId, String action, UUID studentId, String reason) {
        events.publish(new AuditEvent(staffId, action, LossReporter.class.getSimpleName(), studentId, reason));
    }
}
