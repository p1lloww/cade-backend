package br.ucsal.cade.founditems.api.candidate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Objetos que ainda podem ser convocados (Disponível ou Não reclamado), para o cruzamento (RF20). */
public interface MatchableFoundItems {

    /** Vazio se o objeto não existe ou não pode mais ser convocado. */
    Optional<MatchableFoundItem> find(UUID foundItemId);

    List<MatchableFoundItem> findByCategory(UUID categoryId);
}
