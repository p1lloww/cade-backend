package br.ucsal.cade.founditems.persistence.item;

import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @EntityGraph(attributePaths = "category")
    List<FoundItem> findWithCategoryByIdIn(Collection<UUID> ids);

    @Query(nativeQuery = true, value = "SELECT c.name AS category, to_char(f.found_on, 'YYYY-MM') AS month,"
            + " count(*) AS total FROM found_items f JOIN categories c ON c.id = f.category_id"
            + " WHERE f.status IN (:statuses) GROUP BY c.name, to_char(f.found_on, 'YYYY-MM')")
    List<CountByCategoryAndMonth> countByCategoryAndMonth(@Param("statuses") Collection<String> statuses);
}
