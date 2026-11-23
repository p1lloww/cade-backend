package br.ucsal.cade.founditems.persistence.item;

import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import br.ucsal.cade.shared.category.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FoundItemRepository extends JpaRepository<FoundItem, UUID> {

    /** Configuração de busca {@code portuguese} sem acentos, criada pelo {@link FoundItemTextIndex}. */
    String TEXT_CONFIG = "cade_pt";

    /**
     * O texto pesquisável do objeto, igual à expressão do índice GIN ({@link FoundItemTextIndex}): a busca só usa o
     * índice se repetir a expressão exatamente.
     */
    String TEXT_VECTOR = "to_tsvector('" + TEXT_CONFIG + "', coalesce(f.internal_description, '') || ' ' || "
            + "coalesce(f.reserved_details, ''))";

    String TEXT_QUERY = "websearch_to_tsquery('" + TEXT_CONFIG + "', CAST(:text AS text))";

    /** Filtro nulo não filtra. Os valores chegam como texto, para o PostgreSQL saber o tipo mesmo quando são nulos. */
    String SEARCH_FILTERS = " FROM found_items f"
            + " WHERE (CAST(:text AS text) IS NULL OR " + TEXT_VECTOR + " @@ " + TEXT_QUERY + ")"
            + " AND (CAST(:categoryId AS uuid) IS NULL OR f.category_id = CAST(:categoryId AS uuid))"
            + " AND (CAST(:monthStart AS date) IS NULL"
            + "      OR (f.found_on >= CAST(:monthStart AS date) AND f.found_on < CAST(:nextMonthStart AS date)))"
            + " AND (CAST(:block AS text) IS NULL OR f.block = CAST(:block AS text))"
            + " AND (CAST(:room AS text) IS NULL OR f.room = CAST(:room AS text))"
            + " AND f.status IN (:statuses)";

    /**
     * Os ids da página, na ordem da busca (RF10): com texto, os mais relevantes primeiro; sem texto, os achados mais
     * recentemente.
     */
    @Query(nativeQuery = true,
            value = "SELECT f.id" + SEARCH_FILTERS + " ORDER BY CASE WHEN CAST(:text AS text) IS NULL THEN 0"
                    + " ELSE ts_rank(" + TEXT_VECTOR + ", " + TEXT_QUERY + ") END DESC,"
                    + " f.found_on DESC, f.registered_at DESC",
            countQuery = "SELECT count(*)" + SEARCH_FILTERS)
    Page<UUID> searchIds(@Param("text") String text, @Param("categoryId") String categoryId,
                         @Param("monthStart") String monthStart, @Param("nextMonthStart") String nextMonthStart,
                         @Param("block") String block, @Param("room") String room,
                         @Param("statuses") Collection<String> statuses, Pageable pageable);

    @Query("SELECT DISTINCT c FROM FoundItem f JOIN f.category c ORDER BY c.name")
    List<Category> findCategoriesInUse();

    @Query(nativeQuery = true, value = "SELECT DISTINCT f.block AS block, f.room AS room FROM found_items f"
            + " ORDER BY f.block, f.room")
    List<PlaceInUse> findPlacesInUse();

    @Query(nativeQuery = true, value = "SELECT DISTINCT to_char(f.found_on, 'YYYY-MM') FROM found_items f"
            + " ORDER BY 1 DESC")
    List<String> findMonthsInUse();

    /** O objeto já com a categoria carregada, para montar a resposta fora da transação. */
    @EntityGraph(attributePaths = "category")
    Optional<FoundItem> findWithCategoryById(UUID id);

    @EntityGraph(attributePaths = "category")
    Optional<FoundItem> findWithCategoryByIdAndStatusIn(UUID id, Collection<FoundItemStatus> statuses);

    @EntityGraph(attributePaths = "category")
    List<FoundItem> findWithCategoryByCategoryIdAndStatusIn(UUID categoryId, Collection<FoundItemStatus> statuses);

    /** Os objetos de uma situação que entraram na sala até {@code cutoff}, os mais antigos primeiro (RN16). */
    List<FoundItem> findByStatusAndRegisteredAtLessThanEqualOrderByRegisteredAtAsc(FoundItemStatus status,
                                                                                   Instant cutoff);

    @EntityGraph(attributePaths = "category")
    List<FoundItem> findWithCategoryByStatusOrderByStorageBoxAscFoundOnAscRegisteredAtAsc(FoundItemStatus status);

    @EntityGraph(attributePaths = "category")
    List<FoundItem> findWithCategoryByIdIn(Collection<UUID> ids);

    @Query(nativeQuery = true, value = "SELECT c.name AS category, to_char(f.found_on, 'YYYY-MM') AS month,"
            + " count(*) AS total FROM found_items f JOIN categories c ON c.id = f.category_id"
            + " WHERE f.status IN (:statuses) GROUP BY c.name, to_char(f.found_on, 'YYYY-MM')")
    List<CountByCategoryAndMonth> countByCategoryAndMonth(@Param("statuses") Collection<String> statuses);
}
