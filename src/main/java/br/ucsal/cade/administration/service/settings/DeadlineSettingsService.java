package br.ucsal.cade.administration.service.settings;

import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.settings.DeadlineSettings;
import br.ucsal.cade.shared.settings.DeadlineSettingsRepository;
import br.ucsal.cade.shared.settings.DeadlineValues;
import br.ucsal.cade.shared.settings.Deadlines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O administrador ajusta os prazos de guarda, de retirada e de validade do registro (RF45). Cada limite existe para um
 * erro de digitação não travar o sistema (por exemplo, 0 dia de retirada vence a convocação na hora).
 */
@Service
public class DeadlineSettingsService {

    public static final int MIN_STORAGE_DAYS = 30;
    public static final int MAX_STORAGE_DAYS = 365;
    public static final int MIN_SUMMONS_DAYS = 1;
    public static final int MAX_SUMMONS_DAYS = 30;
    public static final int MIN_VALIDITY_DAYS = 7;
    public static final int MAX_VALIDITY_DAYS = 90;

    static final String UPDATED_ACTION = "DEADLINES_UPDATED";
    /** O ajuste é um só, então o histórico o identifica por um id fixo. */
    static final UUID SETTINGS_ID = new UUID(0L, DeadlineSettings.SINGLE_ID);

    private static final Logger log = LoggerFactory.getLogger(DeadlineSettingsService.class);

    private final DeadlineSettingsRepository settings;
    private final Deadlines deadlines;
    private final EventPublisher events;
    private final Clock clock;

    DeadlineSettingsService(DeadlineSettingsRepository settings, Deadlines deadlines, EventPublisher events,
                            Clock clock) {
        this.settings = settings;
        this.deadlines = deadlines;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DeadlineValues current() {
        return deadlines.current();
    }

    /** Grava os três prazos e registra no histórico o que mudou (RF43). Nada mudou: não grava nem registra. */
    @Transactional
    public DeadlineValues update(DeadlineValues requested, UUID adminId) {
        validate(requested);
        DeadlineSettings row = settings.findById(DeadlineSettings.SINGLE_ID).orElse(null);
        DeadlineValues before = row == null ? DeadlineValues.INITIAL : row.values();
        List<String> changes = changesBetween(before, requested);
        if (changes.isEmpty()) {
            return before;
        }
        if (row == null) {
            settings.save(new DeadlineSettings(requested, clock.instant(), adminId));
        } else {
            row.change(requested, clock.instant(), adminId);
            settings.save(row);
        }
        events.publish(new AuditEvent(adminId, UPDATED_ACTION, DeadlineSettings.class.getSimpleName(), SETTINGS_ID,
                "Prazos alterados: " + String.join("; ", changes) + "."));
        log.info("Deadlines changed by {}: {}", adminId, changes);
        return requested;
    }

    private static void validate(DeadlineValues values) {
        require(values.storageDays(), MIN_STORAGE_DAYS, MAX_STORAGE_DAYS, "A guarda");
        require(values.summonsDays(), MIN_SUMMONS_DAYS, MAX_SUMMONS_DAYS, "A retirada depois da convocação");
        require(values.reportValidityDays(), MIN_VALIDITY_DAYS, MAX_VALIDITY_DAYS, "A validade do registro");
    }

    private static void require(int days, int min, int max, String what) {
        if (days < min || days > max) {
            throw new BadRequestException(what + " deve ficar entre " + min + " e " + max + " dias.");
        }
    }

    private static List<String> changesBetween(DeadlineValues before, DeadlineValues after) {
        List<String> changes = new ArrayList<>();
        if (before.storageDays() != after.storageDays()) {
            changes.add("guarda " + before.storageDays() + " → " + after.storageDays() + " dias");
        }
        if (before.summonsDays() != after.summonsDays()) {
            changes.add("retirada " + before.summonsDays() + " → " + after.summonsDays() + " dias");
        }
        if (before.reportValidityDays() != after.reportValidityDays()) {
            changes.add("validade do registro " + before.reportValidityDays() + " → " + after.reportValidityDays()
                    + " dias");
        }
        return changes;
    }
}
