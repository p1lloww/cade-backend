package br.ucsal.cade.founditems.service.item;

import br.ucsal.cade.founditems.api.MatchableFoundItem;
import br.ucsal.cade.founditems.api.MatchableFoundItems;
import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class MatchableFoundItemsService implements MatchableFoundItems {

    private final FoundItemRepository items;

    MatchableFoundItemsService(FoundItemRepository items) {
        this.items = items;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MatchableFoundItem> find(UUID foundItemId) {
        return items.findWithCategoryByIdAndStatusIn(foundItemId, FoundItemStatus.MATCHABLE)
                .map(MatchableFoundItemsService::toMatchable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchableFoundItem> findByCategory(UUID categoryId) {
        return items.findWithCategoryByCategoryIdAndStatusIn(categoryId, FoundItemStatus.MATCHABLE).stream()
                .map(MatchableFoundItemsService::toMatchable)
                .toList();
    }

    private static MatchableFoundItem toMatchable(FoundItem item) {
        return new MatchableFoundItem(item.getId(), item.getCategory().getId(), item.getCategory().getName(),
                item.getFoundOn(), item.getBlock(), item.getRoom(), item.getInternalDescription());
    }
}
