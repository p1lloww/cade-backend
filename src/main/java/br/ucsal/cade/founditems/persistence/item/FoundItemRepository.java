package br.ucsal.cade.founditems.persistence.item;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FoundItemRepository extends JpaRepository<FoundItem, UUID> {

    /** O objeto já com a categoria carregada, para montar a resposta fora da transação. */
    @EntityGraph(attributePaths = "category")
    Optional<FoundItem> findWithCategoryById(UUID id);
}
