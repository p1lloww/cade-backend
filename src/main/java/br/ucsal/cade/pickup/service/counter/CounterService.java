package br.ucsal.cade.pickup.service.counter;

import br.ucsal.cade.founditems.api.FoundItemReservations;
import br.ucsal.cade.lossreports.api.LossReportSummons;
import br.ucsal.cade.lossreports.api.RecordedRefusal;
import br.ucsal.cade.lossreports.api.StudentRefusals;
import br.ucsal.cade.matching.api.SuggestionSummons;
import br.ucsal.cade.pickup.api.event.PickupRegisteredEvent;
import br.ucsal.cade.pickup.exception.SummonsNotFoundException;
import br.ucsal.cade.pickup.persistence.pickup.CounterCheck;
import br.ucsal.cade.pickup.persistence.pickup.Pickup;
import br.ucsal.cade.pickup.persistence.pickup.PickupRepository;
import br.ucsal.cade.pickup.persistence.status.SummonsStatus;
import br.ucsal.cade.pickup.persistence.summons.Summons;
import br.ucsal.cade.pickup.persistence.summons.SummonsRepository;
import br.ucsal.cade.pickup.service.verification.VerificationRules;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * O balcão: o que conferir conforme o risco (RF28), a retirada (RF29) e a recusa (RF31). Retirada e recusa mudam a
 * convocação, o objeto, a perda e a sugestão na mesma transação (seção 7.1), com cada mudança no histórico (RF43).
 *
 * <p>A convocação é gravada primeiro: assim uma retirada e uma recusa simultâneas (ou o vencimento do RET-02) disputam
 * a mesma linha antes de tocar nos outros módulos, e a segunda perde no {@code @Version} sem mudar nada.
 */
@Service
public class CounterService {

    static final String COMPLETED_ACTION = "SUMMONS_COMPLETED";
    static final String COMPLETED_REASON = "Objeto retirado pelo estudante no balcão.";
    static final String PICKUP_ACTION = "PICKUP_REGISTERED";
    static final String PICKUP_REASON = "Retirada registrada com documento, matrícula e verificação conferidos.";
    static final String REFUSED_ACTION = "SUMMONS_REFUSED";
    static final String RELEASE_REASON = "A verificação falhou no balcão; o objeto volta a ficar disponível.";

    private static final Logger log = LoggerFactory.getLogger(CounterService.class);

    private final SummonsRepository summonses;
    private final PickupRepository pickups;
    private final VerificationRules rules;
    private final FoundItemReservations foundItems;
    private final LossReportSummons lossReports;
    private final SuggestionSummons suggestions;
    private final StudentRefusals refusals;
    private final EventPublisher events;
    private final Clock clock;

    CounterService(SummonsRepository summonses, PickupRepository pickups, VerificationRules rules,
                   FoundItemReservations foundItems, LossReportSummons lossReports, SuggestionSummons suggestions,
                   StudentRefusals refusals, EventPublisher events, Clock clock) {
        this.summonses = summonses;
        this.pickups = pickups;
        this.rules = rules;
        this.foundItems = foundItems;
        this.lossReports = lossReports;
        this.suggestions = suggestions;
        this.refusals = refusals;
        this.events = events;
        this.clock = clock;
    }

    /** As convocações Ativas, das que vencem primeiro para as últimas. */
    @Transactional(readOnly = true)
    public List<SummonsAtCounter> active() {
        return summonses.findByStatusOrderByExpiresAtAsc(SummonsStatus.ACTIVE).stream().map(this::atCounter).toList();
    }

    /** A convocação com o que conferir conforme o risco do objeto (RF28). */
    @Transactional(readOnly = true)
    public SummonsAtCounter find(UUID summonsId) {
        return atCounter(load(summonsId));
    }

    /**
     * A verificação é conferida contra a regra do risco antes de qualquer mudança: se ela não basta, nada é gravado.
     *
     * @param staffId id do funcionário (claim {@code sub} do token)
     */
    @Transactional
    public Pickup registerPickup(UUID summonsId, CounterCheck check, UUID staffId) {
        log.debug("Registering pickup for summons {} by {}", summonsId, staffId);
        Summons summons = load(summonsId);
        rules.forRisk(summons.getRisk()).requireAccepted(check.verification());

        Instant now = clock.instant();
        close(summons, s -> s.complete(staffId, now), "Pickup");
        foundItems.handOver(summons.getFoundItemId(), staffId);
        suggestions.complete(summons.getSuggestionId(), staffId);
        lossReports.complete(summons.getLossReportId(), staffId);
        Pickup pickup = pickups.save(Pickup.of(summons, check, staffId, now));

        audit(staffId, COMPLETED_ACTION, Summons.class, summons.getId(), COMPLETED_REASON);
        audit(staffId, PICKUP_ACTION, Pickup.class, pickup.getId(), PICKUP_REASON);
        events.publish(new PickupRegisteredEvent(pickup.getId(), summons.getId(), summons.getStudentId(),
                summons.getFoundItemId(), summons.getLossReportId()));
        logOutcomeAfterCommit(summons, "Pickup " + pickup.getId() + " registered");
        return pickup;
    }

    /**
     * O objeto volta a Disponível, a perda a Aberto e as sugestões Em espera do objeto a Nova, para o funcionário
     * convocar a próxima (RN18). A recusa conta para o estudante (RN12).
     *
     * @param reason por que a verificação falhou, para o histórico
     */
    @Transactional
    public RefusedSummons refuse(UUID summonsId, String reason, UUID staffId) {
        log.debug("Refusing summons {} by {}", summonsId, staffId);
        Summons summons = load(summonsId);

        close(summons, s -> s.refuse(staffId, clock.instant(), reason), "Refusal");
        foundItems.release(summons.getFoundItemId(), staffId, RELEASE_REASON);
        List<UUID> resumed = suggestions.refuse(summons.getSuggestionId(), staffId);
        lossReports.release(summons.getLossReportId(), staffId, RELEASE_REASON);
        RecordedRefusal refusal = refusals.record(summons.getStudentId(), staffId);

        audit(staffId, REFUSED_ACTION, Summons.class, summons.getId(), reason);
        logOutcomeAfterCommit(summons, "Summons " + summons.getId() + " refused (student refusals="
                + refusal.refusals() + ", blocked=" + refusal.blocked() + ", resumed=" + resumed.size() + ")");
        return new RefusedSummons(summons, refusal, resumed);
    }

    private SummonsAtCounter atCounter(Summons summons) {
        return new SummonsAtCounter(summons, rules.forRisk(summons.getRisk()));
    }

    /** O UPDATE com o {@code @Version} sai já aqui, antes dos outros módulos. */
    private void close(Summons summons, Consumer<Summons> transition, String operation) {
        try {
            transition.accept(summons);
        } catch (InvalidStatusTransitionException e) {
            log.info("{} refused for summons {}: status is {}", operation, summons.getId(),
                    summons.getStatus().code());
            throw e;
        }
        summonses.saveAndFlush(summons);
    }

    private Summons load(UUID summonsId) {
        return summonses.findById(summonsId).orElseThrow(() -> {
            log.info("Summons {} not found", summonsId);
            return new SummonsNotFoundException();
        });
    }

    private void audit(UUID staffId, String action, Class<?> entity, UUID entityId, String reason) {
        events.publish(new AuditEvent(staffId, action, entity.getSimpleName(), entityId, reason));
    }

    private void logOutcomeAfterCommit(Summons summons, String outcome) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    log.info("{}: item={}, report={}", outcome, summons.getFoundItemId(), summons.getLossReportId());
                } else {
                    log.warn("Counter decision on summons {} rolled back at commit (probably a simultaneous one)",
                            summons.getId());
                }
            }
        });
    }
}
