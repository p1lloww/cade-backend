package br.ucsal.cade.founditems.api;

import java.util.List;

/** Contagens genéricas dos objetos que estão na sala e podem ser retirados, para a página pública (RF41). */
public interface AvailableItemCounts {

    /**
     * Quantos objetos Disponíveis ou Não reclamados (RN17) há por categoria e mês em que foram achados. Só o nome da
     * categoria, o mês e a quantidade: nada que permita reconhecer um objeto (RN03).
     */
    List<CategoryMonthCount> byCategoryAndMonth();
}
