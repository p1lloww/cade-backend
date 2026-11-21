package br.ucsal.cade.founditems.api;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Objetos em qualquer situação, para a comparação lado a lado com as perdas (RF23). */
public interface ComparableFoundItems {

    /** Numa consulta só; um id que não existe fica fora do mapa. */
    Map<UUID, ComparableFoundItem> findAll(Collection<UUID> foundItemIds);
}
