package br.ucsal.cade.founditems.service.count;

import br.ucsal.cade.founditems.api.AvailableItemCounts;
import br.ucsal.cade.founditems.api.CategoryMonthCount;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
class AvailableItemCountsService implements AvailableItemCounts {

    /** Os que estão na sala e podem ser retirados; Reservado já está prometido a um convocado. */
    static final List<FoundItemStatus> IN_THE_ROOM = List.of(FoundItemStatus.AVAILABLE, FoundItemStatus.UNCLAIMED);

    private final FoundItemRepository items;

    AvailableItemCountsService(FoundItemRepository items) {
        this.items = items;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryMonthCount> byCategoryAndMonth() {
        return items.countByCategoryAndMonth(IN_THE_ROOM.stream().map(FoundItemStatus::code).toList()).stream()
                .map(row -> new CategoryMonthCount(row.getCategory(), YearMonth.parse(row.getMonth()), row.getTotal()))
                .toList();
    }
}
