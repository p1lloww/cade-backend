package br.ucsal.cade.lossreports.service.reporter;

import br.ucsal.cade.lossreports.persistence.reporter.LossReporter;
import br.ucsal.cade.lossreports.persistence.reporter.LossReporterRepository;
import br.ucsal.cade.lossreports.persistence.reporter.StudentNotBlockedException;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * O administrador vê quem está bloqueado por 3 recusas no balcão e libera (RN12, D26). A liberação lê a linha do
 * estudante com a mesma trava do registro de perda e da recusa: uma liberação e uma recusa simultâneas não passam as
 * duas com a contagem antiga.
 */
@Service
public class StudentReleaseService {

    static final String RELEASED_ACTION = "STUDENT_RELEASED";

    private static final Logger log = LoggerFactory.getLogger(StudentReleaseService.class);

    private final LossReporterRepository reporters;
    private final EventPublisher events;

    StudentReleaseService(LossReporterRepository reporters, EventPublisher events) {
        this.reporters = reporters;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<LossReporter> blocked() {
        List<LossReporter> blocked = reporters.findByRefusalsGreaterThanEqualOrderByRefusalsDesc(
                LossReporter.MAX_REFUSALS);
        log.debug("Listed {} blocked students", blocked.size());
        return blocked;
    }

    /**
     * As recusas voltam a zero, e o estudante volta a registrar perdas. Vai para o histórico (RF43) com o
     * administrador como autor e o motivo.
     *
     * @param adminId id do administrador (claim {@code sub} do token)
     * @throws LossReporterNotFoundException se o estudante nunca registrou perda nem teve recusa
     * @throws StudentNotBlockedException    se o estudante não está bloqueado
     */
    @Transactional
    public LossReporter release(UUID studentId, String reason, UUID adminId) {
        LossReporter reporter = reporters.findLockedByStudentId(studentId).orElseThrow(() -> {
            log.info("Release refused: student {} not found", studentId);
            return new LossReporterNotFoundException();
        });
        int refusals = reporter.getRefusals();
        try {
            reporter.release();
        } catch (StudentNotBlockedException e) {
            log.info("Release refused: student {} is not blocked ({} refusals)", studentId, refusals);
            throw e;
        }
        events.publish(new AuditEvent(adminId, RELEASED_ACTION, LossReporter.class.getSimpleName(), studentId,
                "Estudante liberado após " + refusals + " recusas no balcão: " + reason));
        log.info("Student {} released by {} after {} refusals", studentId, adminId, refusals);
        return reporter;
    }
}
