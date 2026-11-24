package br.ucsal.cade.lossreports.service.report;

import br.ucsal.cade.lossreports.api.event.LossReportRegisteredEvent;
import br.ucsal.cade.lossreports.api.event.LossReportUpdatedEvent;
import br.ucsal.cade.lossreports.exception.LossReportNotFoundException;
import br.ucsal.cade.lossreports.exception.StudentBlockedException;
import br.ucsal.cade.lossreports.exception.TooManyActiveReportsException;
import br.ucsal.cade.lossreports.persistence.period.LostPeriod;
import br.ucsal.cade.lossreports.persistence.report.LossReport;
import br.ucsal.cade.lossreports.persistence.report.LossReportRepository;
import br.ucsal.cade.lossreports.persistence.report.NewLossReport;
import br.ucsal.cade.lossreports.persistence.reporter.LossReporter;
import br.ucsal.cade.lossreports.persistence.reporter.LossReporterRepository;
import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.UnknownCategoryException;
import br.ucsal.cade.shared.event.EventPublisher;
import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import br.ucsal.cade.shared.settings.Deadlines;
import br.ucsal.cade.shared.exception.PhotoNotFoundException;
import br.ucsal.cade.shared.storage.PhotoStorage;
import br.ucsal.cade.shared.storage.StoredPhoto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class LossReportService {

    private static final Logger log = LoggerFactory.getLogger(LossReportService.class);

    public static final int MAX_ACTIVE_REPORTS = 5;

    static final String REGISTERED_ACTION = "LOSS_REPORT_REGISTERED";
    static final String REGISTERED_REASON = "Perda registrada pelo estudante.";
    static final String PROOF_ACTION = "LOSS_REPORT_PROOF_ATTACHED";
    static final String PROOF_REASON = "Prova anexada pelo estudante.";
    static final String UPDATED_ACTION = "LOSS_REPORT_UPDATED";
    static final String UPDATED_REASON = "Registro corrigido pelo estudante. Campos alterados: ";

    private final LossReportRepository reports;
    private final LossReporterRepository reporters;
    private final CategoryRepository categories;
    private final PhotoStorage photos;
    private final EventPublisher events;
    private final Clock clock;
    private final Deadlines deadlines;

    LossReportService(LossReportRepository reports, LossReporterRepository reporters, CategoryRepository categories,
                      PhotoStorage photos, EventPublisher events, Clock clock,
                      Deadlines deadlines) {
        this.reports = reports;
        this.reporters = reporters;
        this.categories = categories;
        this.photos = photos;
        this.events = events;
        this.clock = clock;
        this.deadlines = deadlines;
    }

    /**
     * No máximo 5 registros em andamento por estudante (RN05), e nenhum depois de 3 recusas no balcão (RN12). O
     * registro vai para o histórico (RF43).
     */
    @Transactional
    public LossReport register(RegisterLossReport request, UUID studentId) {
        log.debug("Registering loss report: student={}, category={}, locations={}, photo={}", studentId,
                request.categoryId(), request.locations().size(), request.hasPhoto());
        Category category = categories.findById(request.categoryId()).orElseThrow(() -> {
            log.info("Loss report refused: unknown category {} (student={})", request.categoryId(), studentId);
            return new UnknownCategoryException(request.categoryId());
        });
        LostPeriod period = LostPeriod.of(request.lostFrom(), request.lostUntil(), request.shift(),
                LocalDate.now(clock));

        long active = requireRoomForAnotherReport(studentId);
        String photoKey = request.hasPhoto() ? savePhoto(request.photo()) : null;

        NewLossReport data = new NewLossReport(category, request.description(), request.secretDetail(),
                request.locations(), period, photoKey);
        LossReport report = reports.save(LossReport.register(data, studentId, clock.instant(),
                deadlines.reportValidity()));

        events.publish(new AuditEvent(studentId, REGISTERED_ACTION, LossReport.class.getSimpleName(),
                report.getId(), REGISTERED_REASON));
        events.publish(new LossReportRegisteredEvent(report.getId(), category.getId(), period.lostFrom(),
                period.lostUntil()));
        logOutcomeAfterCommit(report.getId(), studentId, active + 1);
        return report;
    }

    /**
     * Corrige o registro enquanto ele está Aberto ou Aguardando informações (PER-07); depois da convocação, a descrição
     * e o detalhe ficam travados (RN06) e a resposta é 409. Registro de outro estudante responde como inexistente
     * (RF14).
     *
     * <p>Quem edita e quem convoca disputam a mesma linha pelo {@code @Version}: a versão que o estudante viu precisa
     * ser a atual, e a gravação é feita já aqui dentro, então o que perde recebe 409 e nada é gravado. O histórico diz
     * só quais campos mudaram, nunca o conteúdo (o detalhe é secreto, RF14). Se nada mudou, nada é gravado nem
     * publicado.
     */
    @Transactional
    public LossReport update(UUID reportId, UpdateLossReport request, UUID studentId) {
        log.debug("Updating loss report {}: student={}, version={}", reportId, studentId, request.version());
        LossReport report = reports.findWithCategoryAndLocationsByIdAndStudentId(reportId, studentId)
                .orElseThrow(() -> {
                    log.info("Loss report update refused: report {} not found for student {}", reportId, studentId);
                    return new LossReportNotFoundException();
                });
        if (report.getVersion() != request.version()) {
            log.info("Loss report update refused: report {} is at version {}, the student saw {}", reportId,
                    report.getVersion(), request.version());
            throw new ObjectOptimisticLockingFailureException(LossReport.class, reportId);
        }
        Category category = categories.findById(request.categoryId()).orElseThrow(() -> {
            log.info("Loss report update refused: unknown category {} (student={})", request.categoryId(), studentId);
            return new UnknownCategoryException(request.categoryId());
        });
        LostPeriod period = LostPeriod.of(request.lostFrom(), request.lostUntil(), request.shift(),
                LocalDate.now(clock));

        List<String> changed;
        try {
            changed = report.edit(category, request.description(), request.secretDetail(), request.locations(),
                    period);
        } catch (InvalidStatusTransitionException e) {
            log.info("Loss report update refused for report {}: status is {}", reportId, report.getStatus().code());
            throw e;
        }
        if (changed.isEmpty()) {
            log.debug("Loss report {} update changed nothing", reportId);
            return report;
        }

        reports.saveAndFlush(report);
        events.publish(new AuditEvent(studentId, UPDATED_ACTION, LossReport.class.getSimpleName(), reportId,
                UPDATED_REASON + String.join(", ", changed) + "."));
        events.publish(new LossReportUpdatedEvent(reportId, category.getId(), period.lostFrom(),
                period.lostUntil()));
        log.info("Loss report {} updated by student {}: {}", reportId, studentId, changed);
        return report;
    }

    /** Só os registros do próprio estudante, em qualquer situação, do mais recente para o mais antigo (RF16). */
    @Transactional(readOnly = true)
    public List<LossReport> listOf(UUID studentId) {
        List<LossReport> mine = reports.findWithCategoryAndLocationsByStudentIdOrderByRegisteredAtDesc(studentId);
        log.debug("Listed {} loss reports of student {}", mine.size(), studentId);
        return mine;
    }

    /**
     * Anexa ou troca a prova do registro: foto e/ou número de série (RF15). Pelo menos um dos dois. A foto passa pelas
     * mesmas regras da foto do registro (JPEG, PNG ou WebP, até 5 MB) e é guardada só depois de todas as regras
     * conferidas; a foto trocada é apagada depois do commit. Registro de outro estudante responde como inexistente
     * (RF14). O histórico diz que a prova foi anexada, não o conteúdo.
     */
    @Transactional
    public LossReport attachProof(UUID reportId, UUID studentId, byte[] photo, String serialNumber) {
        boolean hasPhoto = photo != null && photo.length > 0;
        String serial = serialNumber == null || serialNumber.isBlank() ? null : serialNumber.strip();
        if (!hasPhoto && serial == null) {
            throw new BadRequestException("Envie uma foto ou o número de série da prova.");
        }
        if (serial != null && serial.length() > LossReport.MAX_SERIAL_NUMBER_LENGTH) {
            throw new BadRequestException("O número de série pode ter no máximo "
                    + LossReport.MAX_SERIAL_NUMBER_LENGTH + " caracteres.");
        }
        LossReport report = reports.findWithCategoryAndLocationsByIdAndStudentId(reportId, studentId)
                .orElseThrow(() -> {
                    log.info("Proof refused: report {} not found for student {}", reportId, studentId);
                    return new LossReportNotFoundException();
                });
        String newKey = hasPhoto ? savePhoto(photo) : null;
        String replaced;
        try {
            replaced = report.attachProof(newKey, serial);
        } catch (InvalidStatusTransitionException e) {
            log.info("Proof refused for report {}: status is {}", reportId, report.getStatus().code());
            throw e;
        }
        reports.saveAndFlush(report);
        events.publish(new AuditEvent(studentId, PROOF_ACTION, LossReport.class.getSimpleName(), reportId,
                PROOF_REASON));
        if (replaced != null) {
            deleteAfterCommit(replaced);
        }
        log.info("Proof attached to loss report {} by student {} (photo={}, serial={})", reportId, studentId,
                hasPhoto, serial != null);
        return report;
    }

    /**
     * A foto da prova, para o dono ou para o funcionário que confere (RF15, RF23). Nunca tem URL pública (D21, RN03).
     * O dono de outro registro responde como inexistente (RF14); o funcionário lê o de qualquer estudante.
     */
    @Transactional(readOnly = true)
    public StoredPhoto proofOf(UUID reportId, UUID requesterId, boolean staff) {
        LossReport report = (staff ? reports.findById(reportId) : reports.findByIdAndStudentId(reportId, requesterId))
                .orElseThrow(() -> {
                    log.info("Proof photo refused: report {} not found for {}", reportId, requesterId);
                    return new LossReportNotFoundException();
                });
        if (!report.hasProofPhoto()) {
            throw new PhotoNotFoundException();
        }
        return photos.load(report.getProofKey()).orElseThrow(() -> {
            log.warn("Loss report {} points to proof photo {}, which is missing from the storage", reportId,
                    report.getProofKey());
            return new PhotoNotFoundException();
        });
    }

    /** Registro de outro estudante responde como inexistente (RF14). */
    @Transactional(readOnly = true)
    public StoredPhoto photoOf(UUID reportId, UUID studentId) {
        LossReport report = reports.findByIdAndStudentId(reportId, studentId).orElseThrow(() -> {
            log.info("Loss report photo refused: report {} not found for student {}", reportId, studentId);
            return new LossReportNotFoundException();
        });
        if (!report.hasPhoto()) {
            throw new PhotoNotFoundException();
        }
        return photos.load(report.getPhotoKey()).orElseThrow(() -> {
            log.warn("Loss report {} points to photo {}, which is missing from the storage", reportId,
                    report.getPhotoKey());
            return new PhotoNotFoundException();
        });
    }

    private String savePhoto(byte[] photo) {
        String key = photos.save(photo);
        deleteOnRollback(key);
        log.debug("Loss report photo stored: {} ({} bytes)", key, photo.length);
        return key;
    }

    private void deleteAfterCommit(String photoKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    photos.delete(photoKey);
                    log.debug("Replaced proof photo {} deleted after commit", photoKey);
                }
            }
        });
    }

    private void deleteOnRollback(String photoKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    photos.delete(photoKey);
                    log.debug("Loss report photo {} deleted after rollback", photoKey);
                }
            }
        });
    }

    private long requireRoomForAnotherReport(UUID studentId) {
        reporters.createIfAbsent(studentId);
        LossReporter reporter = reporters.findLockedByStudentId(studentId).orElseThrow();
        if (reporter.isBlocked()) {
            log.info("Loss report refused: student {} is blocked after {} refusals at the counter", studentId,
                    reporter.getRefusals());
            throw new StudentBlockedException();
        }
        long active = reports.countByStudentIdAndStatusIn(studentId, LossReportStatus.ACTIVE);
        if (active >= MAX_ACTIVE_REPORTS) {
            log.info("Loss report refused: student {} already has {} active reports (limit {})", studentId, active,
                    MAX_ACTIVE_REPORTS);
            throw new TooManyActiveReportsException(MAX_ACTIVE_REPORTS);
        }
        return active;
    }

    private void logOutcomeAfterCommit(UUID reportId, UUID studentId, long activeAfter) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    log.info("Loss report {} registered: student={}, active={}/{}", reportId, studentId,
                            activeAfter, MAX_ACTIVE_REPORTS);
                } else {
                    log.warn("Loss report {} rolled back at commit: student={} (probably a simultaneous registration)",
                            reportId, studentId);
                }
            }
        });
    }
}
