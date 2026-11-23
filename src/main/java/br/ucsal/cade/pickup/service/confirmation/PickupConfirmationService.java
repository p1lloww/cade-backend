package br.ucsal.cade.pickup.service.confirmation;

import br.ucsal.cade.pickup.exception.PickupAlreadyConfirmedException;
import br.ucsal.cade.pickup.exception.PickupNotFoundException;
import br.ucsal.cade.pickup.persistence.pickup.Pickup;
import br.ucsal.cade.pickup.persistence.pickup.PickupConfirmation;
import br.ucsal.cade.pickup.persistence.pickup.PickupConfirmationRepository;
import br.ucsal.cade.pickup.persistence.pickup.PickupRepository;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A confirmação da retirada pelo estudante, no próprio celular (RF30): no lugar da assinatura no papel, ele confirma o
 * que o funcionário registrou no balcão. A confirmação é um registro à parte, que só se grava: a retirada continua
 * como o funcionário a registrou (RN19). Quem confirma é sempre o próprio estudante da retirada; a de outro estudante
 * responde como inexistente (RF14).
 */
@Service
public class PickupConfirmationService {

    static final String CONFIRMED_ACTION = "PICKUP_CONFIRMED";
    static final String CONFIRMED_REASON = "O estudante confirmou no celular a retirada registrada no balcão.";

    private static final Logger log = LoggerFactory.getLogger(PickupConfirmationService.class);

    private final PickupRepository pickups;
    private final PickupConfirmationRepository confirmations;
    private final EventPublisher events;
    private final Clock clock;

    PickupConfirmationService(PickupRepository pickups, PickupConfirmationRepository confirmations,
                              EventPublisher events, Clock clock) {
        this.pickups = pickups;
        this.confirmations = confirmations;
        this.events = events;
        this.clock = clock;
    }

    /** As retiradas do estudante, da mais recente para a mais antiga, com a confirmação de cada uma, se houver. */
    @Transactional(readOnly = true)
    public List<StudentPickup> mine(UUID studentId) {
        List<Pickup> mine = pickups.findByStudentIdOrderByHandedOverAtDesc(studentId);
        Map<UUID, PickupConfirmation> confirmed = confirmations
                .findByPickupIdIn(mine.stream().map(Pickup::getId).toList()).stream()
                .collect(Collectors.toMap(PickupConfirmation::getPickupId, Function.identity()));
        return mine.stream()
                .map(pickup -> new StudentPickup(pickup.getId(), pickup.getHandedOverAt(),
                        confirmed.containsKey(pickup.getId()) ? confirmed.get(pickup.getId()).getConfirmedAt() : null))
                .toList();
    }

    /**
     * Grava a confirmação e a registra no histórico (RF43). Confirmar duas vezes responde 409, inclusive duas
     * confirmações simultâneas: a coluna única da retirada faz a segunda perder.
     */
    @Transactional
    public StudentPickup confirm(UUID pickupId, UUID studentId) {
        Pickup pickup = pickups.findByIdAndStudentId(pickupId, studentId).orElseThrow(() -> {
            log.info("Pickup confirmation refused: pickup {} not found for student {}", pickupId, studentId);
            return new PickupNotFoundException();
        });
        if (confirmations.existsByPickupId(pickupId)) {
            log.info("Pickup {} was already confirmed", pickupId);
            throw new PickupAlreadyConfirmedException();
        }
        PickupConfirmation confirmation;
        try {
            confirmation = confirmations.saveAndFlush(PickupConfirmation.of(pickup, clock.instant()));
        } catch (DataIntegrityViolationException e) {
            log.info("Pickup {} was confirmed at the same time by another request", pickupId);
            throw new PickupAlreadyConfirmedException();
        }
        events.publish(new AuditEvent(studentId, CONFIRMED_ACTION, Pickup.class.getSimpleName(), pickupId,
                CONFIRMED_REASON));
        log.info("Pickup {} confirmed by student {}", pickupId, studentId);
        return new StudentPickup(pickupId, pickup.getHandedOverAt(), confirmation.getConfirmedAt());
    }
}
