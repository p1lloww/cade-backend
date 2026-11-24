package br.ucsal.cade.founditems.service.port;

import br.ucsal.cade.founditems.api.reservation.FoundItemReservations;
import br.ucsal.cade.founditems.api.reservation.ReservedFoundItem;
import br.ucsal.cade.founditems.exception.FoundItemNotFoundException;
import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

@Service
class FoundItemReservationsService implements FoundItemReservations {

    static final String RESERVED_ACTION = "FOUND_ITEM_RESERVED";
    static final String RESERVED_REASON = "Objeto reservado para o estudante convocado.";
    static final String HANDED_OVER_ACTION = "FOUND_ITEM_HANDED_OVER";
    static final String HANDED_OVER_REASON = "Objeto retirado pelo dono no balcão.";
    static final String RELEASED_ACTION = "FOUND_ITEM_RELEASED";

    private static final Logger log = LoggerFactory.getLogger(FoundItemReservationsService.class);

    private final FoundItemRepository items;
    private final EventPublisher events;

    FoundItemReservationsService(FoundItemRepository items, EventPublisher events) {
        this.items = items;
        this.events = events;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ReservedFoundItem reserve(UUID foundItemId, UUID staffId) {
        FoundItem item = load(foundItemId, "Reservation");
        change(item, FoundItem::reserve, "Reservation");
        // O UPDATE com o @Version sai já aqui, antes das sugestões: quem convoca o mesmo objeto ao mesmo tempo espera
        // a linha do objeto e perde no @Version, sempre na mesma ordem, sem deadlock.
        items.saveAndFlush(item);
        audit(staffId, RESERVED_ACTION, item, RESERVED_REASON);
        log.debug("Found item {} reserved by {}", foundItemId, staffId);
        return new ReservedFoundItem(item.getId(), item.getRisk());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void handOver(UUID foundItemId, UUID staffId) {
        FoundItem item = load(foundItemId, "Hand-over");
        change(item, FoundItem::handOver, "Hand-over");
        audit(staffId, HANDED_OVER_ACTION, item, HANDED_OVER_REASON);
        log.debug("Found item {} handed over by {}", foundItemId, staffId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void release(UUID foundItemId, UUID actorId, String reason) {
        FoundItem item = load(foundItemId, "Release");
        change(item, FoundItem::release, "Release");
        audit(actorId, RELEASED_ACTION, item, reason);
        log.debug("Found item {} released by {}", foundItemId, actorId);
    }

    private FoundItem load(UUID foundItemId, String operation) {
        return items.findById(foundItemId).orElseThrow(() -> {
            log.info("{} refused: found item {} not found", operation, foundItemId);
            return new FoundItemNotFoundException();
        });
    }

    private static void change(FoundItem item, Consumer<FoundItem> transition, String operation) {
        try {
            transition.accept(item);
        } catch (InvalidStatusTransitionException e) {
            log.info("{} refused for found item {}: status is {}", operation, item.getId(), item.getStatus().code());
            throw e;
        }
    }

    private void audit(UUID actorId, String action, FoundItem item, String reason) {
        events.publish(new AuditEvent(actorId, action, FoundItem.class.getSimpleName(), item.getId(), reason));
    }
}
