package br.ucsal.cade.founditems.service.port;

import br.ucsal.cade.founditems.api.reservation.FoundItemWalkIns;
import br.ucsal.cade.founditems.api.reservation.WalkInFoundItem;
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

@Service
class FoundItemWalkInsService implements FoundItemWalkIns {

    static final String HANDED_OVER_ACTION = "FOUND_ITEM_HANDED_OVER_AT_COUNTER";
    static final String HANDED_OVER_REASON = "Objeto retirado no balcão sem registro de perda (atalho de balcão).";

    private static final Logger log = LoggerFactory.getLogger(FoundItemWalkInsService.class);

    private final FoundItemRepository items;
    private final EventPublisher events;

    FoundItemWalkInsService(FoundItemRepository items, EventPublisher events) {
        this.items = items;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public WalkInFoundItem find(UUID foundItemId) {
        return view(load(foundItemId, "Walk-in check"));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public WalkInFoundItem handOverWithoutSummons(UUID foundItemId, UUID staffId) {
        FoundItem item = load(foundItemId, "Walk-in hand-over");
        try {
            item.handOverWithoutSummons();
        } catch (InvalidStatusTransitionException e) {
            log.info("Walk-in hand-over refused for found item {}: status is {}", foundItemId,
                    item.getStatus().code());
            throw e;
        }
        // O UPDATE com o @Version sai já aqui: uma convocação simultânea do mesmo objeto perde nesta linha.
        items.saveAndFlush(item);
        events.publish(new AuditEvent(staffId, HANDED_OVER_ACTION, FoundItem.class.getSimpleName(), item.getId(),
                HANDED_OVER_REASON));
        log.debug("Found item {} handed over at the counter without summons by {}", foundItemId, staffId);
        return view(item);
    }

    private FoundItem load(UUID foundItemId, String operation) {
        return items.findById(foundItemId).orElseThrow(() -> {
            log.info("{} refused: found item {} not found", operation, foundItemId);
            return new FoundItemNotFoundException();
        });
    }

    private static WalkInFoundItem view(FoundItem item) {
        return new WalkInFoundItem(item.getId(), item.getRisk(), item.getStatus().code(), item.getStatus().label(),
                item.getStatus().allowsHandOverWithoutSummons());
    }
}
