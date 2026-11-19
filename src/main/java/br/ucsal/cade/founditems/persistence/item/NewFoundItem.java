package br.ucsal.cade.founditems.persistence.item;

import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.RiskLevel;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Dados de um objeto a cadastrar, já com a categoria encontrada.
 *
 * @param storageBox caixa escolhida pelo funcionário; nula para usar a sugerida pela data
 * @param risk       risco escolhido pelo funcionário; nulo para usar o padrão da categoria
 */
public record NewFoundItem(Category category, String block, String room, LocalDate foundOn, String storageBox,
                           RiskLevel risk, String internalDescription, String reservedDetails) {

    public NewFoundItem {
        Objects.requireNonNull(category);
        Objects.requireNonNull(block);
        Objects.requireNonNull(room);
        Objects.requireNonNull(foundOn);
    }
}
