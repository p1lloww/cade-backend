package br.ucsal.cade.founditems.api;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * O que o cruzamento usa de um objeto achado (RN07). Leva a descrição interna, que só o funcionário vê (RN03): o
 * cruzamento nunca a mostra a estudantes.
 *
 * @param internalDescription nula quando o funcionário não escreveu nenhuma
 */
public record MatchableFoundItem(UUID id, UUID categoryId, String categoryName, LocalDate foundOn, String block,
                                 String room, String internalDescription) {

    public MatchableFoundItem {
        Objects.requireNonNull(id);
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(categoryName);
        Objects.requireNonNull(foundOn);
        Objects.requireNonNull(block);
    }
}
