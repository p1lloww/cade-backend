package br.ucsal.cade.lossreports.service.report;

import br.ucsal.cade.lossreports.api.event.LossReportRegisteredEvent;
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
import br.ucsal.cade.shared.settings.Deadlines;
import br.ucsal.cade.shared.exception.PhotoNotFoundException;
import br.ucsal.cade.shared.storage.PhotoStorage;
import br.ucsal.cade.shared.storage.StoredPhoto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    /** Só os registros do próprio estudante, em qualquer situação, do mais recente para o mais antigo (RF16). */
    @Transactional(readOnly = true)
    public List<LossReport> listOf(UUID studentId) {
        List<LossReport> mine = reports.findWithCategoryAndLocationsByStudentIdOrderByRegisteredAtDesc(studentId);
        log.debug("Listed {} loss reports of student {}", mine.size(), studentId);
        return mine;
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
