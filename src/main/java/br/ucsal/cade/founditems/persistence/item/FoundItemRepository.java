package br.ucsal.cade.founditems.persistence.item;

import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FoundItemRepository extends JpaRepository<FoundItem, UUID> {

    /** O objeto já com a categoria carregada, para montar a resposta fora da transação. */
    @EntityGraph(attributePaths = "category")
    Optional<FoundItem> findWithCategoryById(UUID id);

    @EntityGraph(attributePaths = "category")
    Optional<FoundItem> findWithCategoryByIdAndStatusIn(UUID id, Collection<FoundItemStatus> statuses);

    @EntityGraph(attributePaths = "category")
    List<FoundItem> findWithCategoryByCategoryIdAndStatusIn(UUID categoryId, Collection<FoundItemStatus> statuses);
}
