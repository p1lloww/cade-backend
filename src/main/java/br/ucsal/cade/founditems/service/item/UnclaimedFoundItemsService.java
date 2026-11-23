package br.ucsal.cade.founditems.service.item;

import br.ucsal.cade.founditems.api.UnclaimedFoundItem;
import br.ucsal.cade.founditems.api.UnclaimedFoundItems;
import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import br.ucsal.cade.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class UnclaimedFoundItemsService implements UnclaimedFoundItems {

    static final String UNCLAIMED_ACTION = "FOUND_ITEM_MARKED_UNCLAIMED";
    static final String UNCLAIMED_REASON = "Objeto passou do prazo de guarda sem dono e virou não reclamado (RN16).";
    static final String DISPOSED_ACTION = "FOUND_ITEM_DISPOSED";

    private static final Logger log = LoggerFactory.getLogger(UnclaimedFoundItemsService.class);

    private final FoundItemRepository items;
    private final EventPublisher events;

    UnclaimedFoundItemsService(FoundItemRepository items, EventPublisher events) {
        this.items = items;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> idsDueForUnclaimed(Instant registeredUntil) {
        return items.findByStatusAndRegisteredAtLessThanEqualOrderByRegisteredAtAsc(FoundItemStatus.AVAILABLE,
                registeredUntil).stream().map(FoundItem::getId).toList();
    }

    @Override
    @Transactional
    public boolean markUnclaimed(UUID foundItemId) {
        FoundItem item = items.findById(foundItemId).orElse(null);
        if (item == null || item.getStatus() != FoundItemStatus.AVAILABLE) {
            return false;
        }
        item.markUnclaimed();
        // O UPDATE com o @Version sai já aqui: se uma convocação reservou o objeto ao mesmo tempo, esta perde.
        items.saveAndFlush(item);
        events.publish(new AuditEvent(AuditEvent.SYSTEM_ACTOR, UNCLAIMED_ACTION, FoundItem.class.getSimpleName(),
                foundItemId, UNCLAIMED_REASON));
        log.info("Found item {} is now unclaimed", foundItemId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnclaimedFoundItem> listUnclaimed() {
        return items.findWithCategoryByStatusOrderByStorageBoxAscFoundOnAscRegisteredAtAsc(FoundItemStatus.UNCLAIMED)
                .stream().map(UnclaimedFoundItemsService::view).toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UnclaimedFoundItem dispose(UUID foundItemId, UUID adminId, String reason) {
        FoundItem item = items.findWithCategoryById(foundItemId).orElseThrow(() -> {
            log.info("Disposal refused: found item {} not found", foundItemId);
            return new NotFoundException(FoundItemService.NOT_FOUND_MESSAGE);
        });
        try {
            item.dispose();
        } catch (InvalidStatusTransitionException e) {
            log.info("Disposal refused for found item {}: status is {}", foundItemId, item.getStatus().code());
            throw e;
        }
        items.saveAndFlush(item);
        events.publish(new AuditEvent(adminId, DISPOSED_ACTION, FoundItem.class.getSimpleName(), foundItemId, reason));
        return view(item);
    }

    private static UnclaimedFoundItem view(FoundItem item) {
        return new UnclaimedFoundItem(item.getId(), item.getCategory().getName(), item.getBlock(), item.getRoom(),
                item.getFoundOn(), item.getStorageBox(), item.getInternalDescription(), item.getRegisteredAt());
    }
}
