package br.ucsal.cade.founditems.service.count;

import br.ucsal.cade.founditems.api.FoundItemOverview;
import br.ucsal.cade.founditems.api.StatusCategoryCount;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class FoundItemOverviewService implements FoundItemOverview {

    private final FoundItemRepository items;

    FoundItemOverviewService(FoundItemRepository items) {
        this.items = items;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusCategoryCount> countByStatusAndCategory() {
        return items.countByStatusAndCategory().stream()
                .map(row -> {
                    FoundItemStatus status = FoundItemStatus.ALL.stream()
                            .filter(candidate -> candidate.code().equals(row.getStatus())).findFirst().orElseThrow();
                    return new StatusCategoryCount(status.code(), status.label(), row.getCategory(), row.getTotal());
                })
                .toList();
    }
}
