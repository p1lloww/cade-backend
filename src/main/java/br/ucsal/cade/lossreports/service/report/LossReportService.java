package br.ucsal.cade.lossreports.service.report;

import br.ucsal.cade.lossreports.api.event.LossReportRegisteredEvent;
import br.ucsal.cade.lossreports.dto.request.LossLocationRequestDTO;
import br.ucsal.cade.lossreports.dto.request.LossReportRequestDTO;
import br.ucsal.cade.lossreports.dto.response.LossReportResponseDTO;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.persistence.period.LostPeriod;
import br.ucsal.cade.lossreports.persistence.report.LossReport;
import br.ucsal.cade.lossreports.persistence.report.LossReportRepository;
import br.ucsal.cade.lossreports.persistence.report.NewLossReport;
import br.ucsal.cade.lossreports.persistence.reporter.LossReporterRepository;
import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.UnknownCategoryException;
import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.exception.LossReportNotFoundException;
import br.ucsal.cade.shared.exception.PhotoNotFoundException;
import br.ucsal.cade.shared.exception.TooManyActiveReportsException;
import br.ucsal.cade.shared.storage.PhotoStorage;
import br.ucsal.cade.shared.storage.StoredPhoto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Registro de perdas pelo estudante (RF13, RF14). */
@Service
public class LossReportService {

    private static final Logger log = LoggerFactory.getLogger(LossReportService.class);

    /** RN05: dificulta "pescar" objetos com vários registros genéricos. */
    public static final int MAX_ACTIVE_REPORTS = 5;

    static final String REGISTERED_ACTION = "LOSS_REPORT_REGISTERED";
    static final String REGISTERED_REASON = "Perda registrada pelo estudante.";

    private final LossReportRepository reports;
    private final LossReporterRepository reporters;
    private final CategoryRepository categories;
    private final PhotoStorage photos;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    LossReportService(LossReportRepository reports, LossReporterRepository reporters, CategoryRepository categories,
                      PhotoStorage photos, ApplicationEventPublisher events, Clock clock) {
        this.reports = reports;
        this.reporters = reporters;
        this.categories = categories;
        this.photos = photos;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Registra a perda, grava o histórico e avisa que ela foi registrada, tudo na mesma transação (RF43, D10). O
     * pedido chega já validado pelo {@code @Valid} (campos obrigatórios, tamanhos, até 5 locais); aqui ficam só as
     * regras que dependem do banco, do relógio ou dos outros registros.
     *
     * <p>O limite de registros em andamento (RN05) é conferido com o estudante travado pelo lock otimista: se outro
     * registro dele for gravado entre a contagem e o commit, este falha com
     * {@link org.springframework.orm.ObjectOptimisticLockingFailureException} e é desfeito inteiro. O fluxo está em
     * {@code docs/tecnico/limite-registros-abertos.md}.
     *
     * <p>A foto é opcional. Ela só é guardada depois de todas as regras conferidas e é apagada se a transação for
     * desfeita, para não sobrar foto sem registro.
     *
     * <p>Os logs nunca levam a descrição nem o detalhe secreto (RF14), só ids e contagens.
     *
     * @param photo     foto do objeto; nula ou vazia quando o estudante não enviou
     * @param studentId id da conta do estudante (claim {@code sub} do token)
     * @throws UnknownCategoryException      se a categoria não existe
     * @throws BadRequestException           se a data é futura, o período é inválido ou a foto não é JPEG, PNG ou
     *                                       WebP de até 5 MB
     * @throws TooManyActiveReportsException se o estudante já tem {@value #MAX_ACTIVE_REPORTS} registros em andamento
     */
    @Transactional
    public LossReportResponseDTO register(LossReportRequestDTO request, MultipartFile photo, UUID studentId) {
        List<LossLocation> locations = locationsOf(request);
        log.debug("Registering loss report: student={}, category={}, locations={}, photo={}", studentId,
                request.categoryId(), locations.size(), hasContent(photo));
        Category category = categories.findById(request.categoryId()).orElseThrow(() -> {
            log.info("Loss report refused: unknown category {} (student={})", request.categoryId(), studentId);
            return new UnknownCategoryException(request.categoryId());
        });
        LostPeriod period = LostPeriod.of(request.lostFrom(), request.lostUntil(), request.shift(),
                LocalDate.now(clock));

        long active = requireRoomForAnotherReport(studentId);
        String photoKey = hasContent(photo) ? savePhoto(photo) : null;

        NewLossReport data = new NewLossReport(category, request.description(), request.secretDetail(), locations,
                period, photoKey);
        LossReport report = reports.save(LossReport.register(data, studentId, clock.instant()));

        events.publishEvent(new AuditEvent(studentId, REGISTERED_ACTION, LossReport.class.getSimpleName(),
                report.getId(), REGISTERED_REASON));
        events.publishEvent(new LossReportRegisteredEvent(report.getId(), category.getId(), period.lostFrom(),
                period.lostUntil()));
        logOutcomeAfterCommit(report.getId(), studentId, active + 1);
        return LossReportResponseDTO.from(report);
    }

    /**
     * A foto do registro, servida só ao dono (RF14). Registro de outro estudante responde como inexistente.
     *
     * @throws LossReportNotFoundException se o registro não existe ou é de outro estudante
     * @throws PhotoNotFoundException      se o registro não tem foto
     */
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

    private String savePhoto(MultipartFile photo) {
        try {
            String key = photos.save(photo.getBytes());
            deleteOnRollback(key);
            log.debug("Loss report photo stored: {} ({} bytes)", key, photo.getSize());
            return key;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
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

    private static boolean hasContent(MultipartFile photo) {
        return photo != null && !photo.isEmpty();
    }

    private static List<LossLocation> locationsOf(LossReportRequestDTO request) {
        List<LossLocationRequestDTO> locations = request.locations() == null ? List.of() : request.locations();
        return locations.stream().map(location -> LossLocation.of(location.block(), location.room())).toList();
    }

    /**
     * Trava o estudante (lock otimista) antes de contar, para a contagem valer até o commit (RN05).
     *
     * @return quantos registros em andamento o estudante já tem
     */
    private long requireRoomForAnotherReport(UUID studentId) {
        reporters.createIfAbsent(studentId);
        reporters.findLockedByStudentId(studentId).orElseThrow();
        long active = reports.countByStudentIdAndStatusIn(studentId, LossReportStatus.ACTIVE);
        if (active >= MAX_ACTIVE_REPORTS) {
            log.info("Loss report refused: student {} already has {} active reports (limit {})", studentId, active,
                    MAX_ACTIVE_REPORTS);
            throw new TooManyActiveReportsException(MAX_ACTIVE_REPORTS);
        }
        return active;
    }

    /**
     * O resultado só é conhecido no fim da transação: um registro que passou por todas as regras ainda pode ser
     * desfeito no commit pelo lock otimista (outro registro simultâneo do mesmo estudante).
     */
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
