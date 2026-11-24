package br.ucsal.cade.pickup.service.walkin;

import br.ucsal.cade.founditems.api.reservation.FoundItemWalkIns;
import br.ucsal.cade.founditems.api.reservation.WalkInFoundItem;
import br.ucsal.cade.matching.api.FoundItemSuggestions;
import br.ucsal.cade.pickup.api.event.WalkInPickupRegisteredEvent;
import br.ucsal.cade.pickup.exception.EnrollmentRequiredException;
import br.ucsal.cade.pickup.persistence.pickup.CounterCheck;
import br.ucsal.cade.pickup.persistence.walkin.WalkInHolder;
import br.ucsal.cade.pickup.persistence.walkin.WalkInPickup;
import br.ucsal.cade.pickup.persistence.walkin.WalkInPickupRepository;
import br.ucsal.cade.pickup.service.verification.VerificationRules;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * O atalho de balcão (RF32): a retirada de quem veio direto à sala, sem registro de perda, inclusive sem conta (RN20).
 * Objeto e sugestões mudam na mesma transação que grava a retirada, com cada mudança no histórico (RF43).
 *
 * <p>O objeto é gravado primeiro, com o {@code @Version}: um atalho e uma convocação simultâneos do mesmo objeto
 * disputam a mesma linha, e o segundo perde (409) sem mudar nada. Detalhes em {@code docs/tecnico/retirada.md}.
 */
@Service
public class WalkInService {

    static final String REGISTERED_ACTION = "WALK_IN_PICKUP_REGISTERED";
    static final String REGISTERED_REASON = "Retirada sem registro de perda (atalho de balcão), com documento e "
            + "verificação conferidos.";

    private static final Logger log = LoggerFactory.getLogger(WalkInService.class);

    private final WalkInPickupRepository pickups;
    private final FoundItemWalkIns foundItems;
    private final FoundItemSuggestions suggestions;
    private final VerificationRules rules;
    private final EventPublisher events;
    private final Clock clock;

    WalkInService(WalkInPickupRepository pickups, FoundItemWalkIns foundItems, FoundItemSuggestions suggestions,
                  VerificationRules rules, EventPublisher events, Clock clock) {
        this.pickups = pickups;
        this.foundItems = foundItems;
        this.suggestions = suggestions;
        this.rules = rules;
        this.events = events;
        this.clock = clock;
    }

    /** O que conferir conforme o risco do objeto, e se ele pode sair pelo atalho (RF28). */
    @Transactional(readOnly = true)
    public WalkInAtCounter check(UUID foundItemId) {
        WalkInFoundItem item = foundItems.find(foundItemId);
        return new WalkInAtCounter(item, rules.forRisk(item.risk()));
    }

    /**
     * A verificação e a matrícula são conferidas antes de qualquer mudança: se faltar algo, nada é gravado.
     *
     * @param staffId id do funcionário (claim {@code sub} do token)
     */
    @Transactional
    public RegisteredWalkIn register(UUID foundItemId, WalkInHolder holder, CounterCheck check, UUID staffId) {
        log.debug("Registering walk-in pickup of found item {} ({}) by {}", foundItemId, holder.type(), staffId);
        WalkInFoundItem item = foundItems.find(foundItemId);
        rules.forRisk(item.risk()).requireAccepted(check.verification());
        CounterCheck recorded = withEnrollmentFor(holder, check);

        foundItems.handOverWithoutSummons(foundItemId, staffId);
        List<UUID> closed = suggestions.closeOpenOf(foundItemId, staffId);
        WalkInPickup pickup = pickups.save(WalkInPickup.of(foundItemId, holder, recorded, staffId,
                clock.instant()));

        events.publish(new AuditEvent(staffId, REGISTERED_ACTION, WalkInPickup.class.getSimpleName(), pickup.getId(),
                REGISTERED_REASON));
        events.publish(new WalkInPickupRegisteredEvent(pickup.getId(), foundItemId, closed));
        logOutcomeAfterCommit(pickup, closed.size());
        return new RegisteredWalkIn(pickup, closed);
    }

    /** Estudante sem matrícula é recusado (RN10); a de quem não tem conta é descartada, se vier. */
    private static CounterCheck withEnrollmentFor(WalkInHolder holder, CounterCheck check) {
        if (!holder.type().requiresEnrollment()) {
            return new CounterCheck(check.documentType(), check.documentNumber(), null, check.verification(),
                    check.notes());
        }
        if (check.enrollment() == null || check.enrollment().isBlank()) {
            log.info("Walk-in pickup refused: a student without enrollment");
            throw new EnrollmentRequiredException();
        }
        return check;
    }

    private void logOutcomeAfterCommit(WalkInPickup pickup, int closed) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    log.info("Walk-in pickup {} registered: item={}, holder={}, closedSuggestions={}",
                            pickup.getId(), pickup.getFoundItemId(), pickup.getHolder().type(), closed);
                } else {
                    log.warn("Walk-in pickup of found item {} rolled back at commit (probably a simultaneous summons)",
                            pickup.getFoundItemId());
                }
            }
        });
    }
}
