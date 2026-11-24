package br.ucsal.cade.founditems.api.count;

import java.util.List;

/** As contagens dos objetos para o painel do administrador (RF46), chamada pelo módulo de administração. */
public interface FoundItemOverview {

    /** Quantos objetos há em cada situação e categoria; só as combinações que têm objeto. */
    List<StatusCategoryCount> countByStatusAndCategory();
}
