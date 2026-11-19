package br.ucsal.cade.lossreports.service.report;

import br.ucsal.cade.lossreports.api.event.LossReportRegisteredEvent;
import br.ucsal.cade.lossreports.persistence.period.LostPeriod;
import br.ucsal.cade.lossreports.persistence.report.LossReport;
import br.ucsal.cade.lossreports.persistence.report.LossReportRepository;
import br.ucsal.cade.lossreports.persistence.report.NewLossReport;
import br.ucsal.cade.lossreports.persistence.reporter.LossReporterRepository;
import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.exception.ConflictException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/** Registro de perdas pelo estudante (RF13, RF14). */
@Service
public class LossReportService {

    /** RN05: dificulta "pescar" objetos com vários registros genéricos. */
    public static final int MAX_ACTIVE_REPORTS = 5;

    static final String REGISTERED_ACTION = "LOSS_REPORT_REGISTERED";
    static final String REGISTERED_REASON = "Perda registrada pelo estudante.";
    static final String UNKNOWN_CATEGORY_MESSAGE = "Categoria não encontrada.";
    static final String LIMIT_MESSAGE = "Você já tem " + MAX_ACTIVE_REPORTS + " registros em andamento. Cancele um "
            + "que não precisa mais, ou espere um deles ser resolvido, para registrar outra perda.";

    private final LossReportRepository reports;
    private final LossReporterRepository reporters;
    private final CategoryRepository categories;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    LossReportService(LossReportRepository reports, LossReporterRepository reporters, CategoryRepository categories,
                      ApplicationEventPublisher events, Clock clock) {
        this.reports = reports;
        this.reporters = reporters;
        this.categories = categories;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Registra a perda, grava o histórico e avisa que ela foi registrada, tudo na mesma transação (RF43, D10).
     *
     * <p>O limite de registros em andamento (RN05) é conferido com o estudante travado pelo lock otimista: se outro
     * registro dele for gravado entre a contagem e o commit, este falha com
     * {@link org.springframework.orm.ObjectOptimisticLockingFailureException} e é desfeito inteiro. O fluxo está em
     * {@code docs/tecnico/limite-registros-abertos.md}.
     *
     * @param studentId id da conta do estudante (claim {@code sub} do token)
     * @throws BadRequestException se a categoria não existe ou os dados violam uma regra do registro
     * @throws ConflictException   se o estudante já tem {@value #MAX_ACTIVE_REPORTS} registros em andamento
     */
    @Transactional
    public LossReport register(RegisterLossReport request, UUID studentId) {
        Category category = categories.findById(request.categoryId())
                .orElseThrow(() -> new BadRequestException(UNKNOWN_CATEGORY_MESSAGE));
        LostPeriod period = LostPeriod.of(request.lostFrom(), request.lostUntil(), request.shift(),
                LocalDate.now(clock));

        requireRoomForAnotherReport(studentId);

        NewLossReport data = new NewLossReport(category, request.description(), request.secretDetail(),
                request.locations(), period);
        LossReport report = reports.save(LossReport.register(data, studentId, clock.instant()));

        events.publishEvent(new AuditEvent(studentId, REGISTERED_ACTION, LossReport.class.getSimpleName(),
                report.getId(), REGISTERED_REASON));
        events.publishEvent(new LossReportRegisteredEvent(report.getId(), category.getId(), period.lostFrom(),
                period.lostUntil()));
        return report;
    }

    /** Trava o estudante (lock otimista) antes de contar, para a contagem valer até o commit (RN05). */
    private void requireRoomForAnotherReport(UUID studentId) {
        reporters.createIfAbsent(studentId);
        reporters.findLockedByStudentId(studentId).orElseThrow();
        if (reports.countByStudentIdAndStatusIn(studentId, LossReportStatus.ACTIVE) >= MAX_ACTIVE_REPORTS) {
            throw new ConflictException(LIMIT_MESSAGE);
        }
    }
}
