package br.ucsal.cade.founditems.service.item;

import br.ucsal.cade.founditems.api.FoundItemReservations;
import br.ucsal.cade.founditems.api.ReservedFoundItem;
import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import br.ucsal.cade.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class FoundItemReservationsService implements FoundItemReservations {

    static final String RESERVED_ACTION = "FOUND_ITEM_RESERVED";
    static final String RESERVED_REASON = "Objeto reservado para o estudante convocado.";

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
        FoundItem item = items.findById(foundItemId).orElseThrow(() -> {
            log.info("Reservation refused: found item {} not found", foundItemId);
            return new NotFoundException(FoundItemService.NOT_FOUND_MESSAGE);
        });
        try {
            item.reserve();
        } catch (InvalidStatusTransitionException e) {
            log.info("Reservation refused for found item {}: status is {}", foundItemId, item.getStatus().code());
            throw e;
        }
        // O UPDATE com o @Version sai já aqui, antes das sugestões: quem convoca o mesmo objeto ao mesmo tempo espera
        // a linha do objeto e perde no @Version, sempre na mesma ordem, sem deadlock.
        items.saveAndFlush(item);
        events.publish(new AuditEvent(staffId, RESERVED_ACTION, FoundItem.class.getSimpleName(), item.getId(),
                RESERVED_REASON));
        log.debug("Found item {} reserved by {}", foundItemId, staffId);
        return new ReservedFoundItem(item.getId(), item.getRisk());
    }
}
