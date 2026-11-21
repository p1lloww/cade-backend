package br.ucsal.cade.founditems.service.item;

import br.ucsal.cade.founditems.api.ComparableFoundItem;
import br.ucsal.cade.founditems.api.ComparableFoundItems;
import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class ComparableFoundItemsService implements ComparableFoundItems {

    private final FoundItemRepository items;

    ComparableFoundItemsService(FoundItemRepository items) {
        this.items = items;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ComparableFoundItem> findAll(Collection<UUID> foundItemIds) {
        if (foundItemIds.isEmpty()) {
            return Map.of();
        }
        return items.findWithCategoryByIdIn(foundItemIds).stream()
                .map(ComparableFoundItemsService::toComparable)
                .collect(Collectors.toMap(ComparableFoundItem::id, Function.identity()));
    }

    private static ComparableFoundItem toComparable(FoundItem item) {
        return new ComparableFoundItem(item.getId(), item.getCategory().getName(), item.getFoundOn(),
                item.getBlock(), item.getRoom(), item.getStorageBox(), item.getRisk(), item.getStatus().label(),
                item.getInternalDescription(), item.getReservedDetails());
    }
}
