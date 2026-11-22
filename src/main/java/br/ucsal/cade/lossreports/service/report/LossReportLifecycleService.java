package br.ucsal.cade.lossreports.service.report;

import br.ucsal.cade.lossreports.api.LossReportInfoRequests;
import br.ucsal.cade.lossreports.api.event.LossReportClosedEvent;
import br.ucsal.cade.lossreports.api.event.LossReportInfoOverdueEvent;
import br.ucsal.cade.lossreports.api.event.LossReportInfoRequestedEvent;
import br.ucsal.cade.lossreports.persistence.report.LossReport;
import br.ucsal.cade.lossreports.persistence.report.LossReportRepository;
import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * O resto do ciclo de vida do registro de perda (seção 6.2 dos requisitos): o estudante cancela (RF17), responde ao
 * pedido de informações (RF18) e renova (RN14); o funcionário pede mais informações (RF24); e o tempo vence o registro
 * (RN14) e o pedido sem resposta (RN13).
 *
 * <p>Cada ação grava a mudança com {@code saveAndFlush}, então uma ação simultânea sobre o mesmo registro (por
 * exemplo, a convocação) perde no {@code @Version} e responde 409. Os vencimentos da tarefa agendada rodam um registro
 * por transação, em nome do sistema.
 */
@Service
public class LossReportLifecycleService implements LossReportInfoRequests {

    static final String CANCELLED_ACTION = "LOSS_REPORT_CANCELLED";
    static final String CANCELLED_REASON = "Registro cancelado pelo estudante: já encontrou o objeto.";
    static final String INFO_REQUESTED_ACTION = "LOSS_REPORT_INFO_REQUESTED";
    static final String INFO_ANSWERED_ACTION = "LOSS_REPORT_INFO_ANSWERED";
    static final String INFO_ANSWERED_REASON = "O estudante respondeu ao pedido de mais informações.";
    static final String INFO_OVERDUE_ACTION = "LOSS_REPORT_INFO_OVERDUE";
    static final String INFO_OVERDUE_REASON =
            "Passaram 5 dias sem resposta ao pedido de informações; o registro volta a ficar aberto (RN13).";
    static final String RENEWED_ACTION = "LOSS_REPORT_RENEWED";
    static final String RENEWED_REASON = "Registro renovado pelo estudante por mais 30 dias (RN14).";
    static final String EXPIRED_ACTION = "LOSS_REPORT_EXPIRED";
    static final String EXPIRED_REASON = "O registro passou de 30 dias sem renovação (RN14).";

    private static final Logger log = LoggerFactory.getLogger(LossReportLifecycleService.class);

    private final LossReportRepository reports;
    private final LossReportProperties properties;
    private final EventPublisher events;
    private final Clock clock;

    LossReportLifecycleService(LossReportRepository reports, LossReportProperties properties, EventPublisher events,
                               Clock clock) {
        this.reports = reports;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
    }

    /** Aberto ou Aguardando informações → Cancelado. As sugestões abertas do registro são encerradas (RF17). */
    @Transactional
    public LossReport cancel(UUID reportId, UUID studentId) {
        LossReport report = ownedBy(reportId, studentId);
        change(report, LossReport::cancel, "Cancel");
        reports.saveAndFlush(report);
        audit(studentId, CANCELLED_ACTION, reportId, CANCELLED_REASON);
        events.publish(new LossReportClosedEvent(reportId, LossReportClosedEvent.Reason.CANCELLED));
        log.info("Loss report {} cancelled by student {}", reportId, studentId);
        return report;
    }

    /**
     * Aguardando informações → Aberto. A resposta fica no registro, para o funcionário ler; o histórico diz só que ela
     * foi dada, porque o texto pode conter o detalhe secreto (RF14).
     */
    @Transactional
    public LossReport respond(UUID reportId, String answer, UUID studentId) {
        LossReport report = ownedBy(reportId, studentId);
        change(report, r -> r.respond(answer), "Answer");
        reports.saveAndFlush(report);
        audit(studentId, INFO_ANSWERED_ACTION, reportId, INFO_ANSWERED_REASON);
        log.info("Loss report {} answered by student {}", reportId, studentId);
        return report;
    }

    /** Mais 30 dias a partir de agora, enquanto o registro está Aberto ou Aguardando informações (RN14). */
    @Transactional
    public LossReport renew(UUID reportId, UUID studentId) {
        LossReport report = ownedBy(reportId, studentId);
        Instant now = clock.instant();
        change(report, r -> r.renew(now), "Renewal");
        reports.saveAndFlush(report);
        audit(studentId, RENEWED_ACTION, reportId, RENEWED_REASON);
        log.info("Loss report {} renewed by student {} until {}", reportId, studentId, report.getExpiresAt());
        return report;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void request(UUID lossReportId, UUID suggestionId, String reason, UUID staffId) {
        LossReport report = reports.findById(lossReportId).orElseThrow(LossReportNotFoundException::new);
        change(report, r -> r.requestInfo(suggestionId, reason, clock.instant()), "Info request");
        reports.saveAndFlush(report);
        audit(staffId, INFO_REQUESTED_ACTION, lossReportId, reason);
        events.publish(new LossReportInfoRequestedEvent(UUID.randomUUID(), lossReportId, report.getStudentId(),
                reason));
        log.info("Info requested on loss report {} by {}", lossReportId, staffId);
    }

    /** Os ids dos registros Abertos cujo prazo de 30 dias já passou (RN14). */
    @Transactional(readOnly = true)
    public List<UUID> dueToExpire() {
        return reports.findByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(LossReportStatus.OPEN,
                clock.instant()).stream().map(LossReport::getId).toList();
    }

    /** Os ids dos registros cujo pedido de informações passou de 5 dias sem resposta (RN13). */
    @Transactional(readOnly = true)
    public List<UUID> infoOverdue() {
        return reports.findByStatusAndInfoRequestedAtLessThanEqualOrderByInfoRequestedAtAsc(
                LossReportStatus.AWAITING_INFO, clock.instant().minus(properties.infoResponseDeadline()))
                .stream().map(LossReport::getId).toList();
    }

    /**
     * Aberto → Vencido, em nome do sistema (RN14). O estado é conferido de novo aqui dentro, porque o estudante pode
     * ter renovado ou o funcionário convocado depois de a tarefa listar o registro.
     *
     * @return se o registro venceu agora
     */
    @Transactional
    public boolean expire(UUID reportId) {
        LossReport report = reports.findById(reportId).orElse(null);
        Instant now = clock.instant();
        if (report == null || report.getStatus() != LossReportStatus.OPEN || report.getExpiresAt().isAfter(now)) {
            return false;
        }
        report.expire();
        reports.saveAndFlush(report);
        audit(AuditEvent.SYSTEM_ACTOR, EXPIRED_ACTION, reportId, EXPIRED_REASON);
        events.publish(new LossReportClosedEvent(reportId, LossReportClosedEvent.Reason.EXPIRED));
        log.info("Loss report {} expired", reportId);
        return true;
    }

    /**
     * Aguardando informações → Aberto sem resposta, em nome do sistema (RN13). A sugestão que originou o pedido é
     * descartada por quem a guarda, ouvindo o evento.
     *
     * @return se o pedido venceu agora
     */
    @Transactional
    public boolean resumeOverdue(UUID reportId) {
        LossReport report = reports.findById(reportId).orElse(null);
        if (report == null || report.getStatus() != LossReportStatus.AWAITING_INFO || report.getInfoRequestedAt() == null
                || report.getInfoRequestedAt().isAfter(clock.instant().minus(properties.infoResponseDeadline()))) {
            return false;
        }
        UUID suggestionId = report.getInfoSuggestionId();
        report.resumeWithoutAnswer();
        reports.saveAndFlush(report);
        audit(AuditEvent.SYSTEM_ACTOR, INFO_OVERDUE_ACTION, reportId, INFO_OVERDUE_REASON);
        if (suggestionId != null) {
            events.publish(new LossReportInfoOverdueEvent(reportId, suggestionId));
        }
        log.info("Loss report {} back to open: no answer to the info request", reportId);
        return true;
    }

    /** Registro de outro estudante responde como inexistente (RF14). */
    private LossReport ownedBy(UUID reportId, UUID studentId) {
        return reports.findWithCategoryAndLocationsByIdAndStudentId(reportId, studentId).orElseThrow(() -> {
            log.info("Loss report {} not found for student {}", reportId, studentId);
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

    private void audit(UUID actorId, String action, UUID reportId, String reason) {
        events.publish(new AuditEvent(actorId, action, LossReport.class.getSimpleName(), reportId, reason));
    }
}
