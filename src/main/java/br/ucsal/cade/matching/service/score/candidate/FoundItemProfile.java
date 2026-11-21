package br.ucsal.cade.matching.service.score.candidate;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * O que o cruzamento usa de um objeto achado.
 *
 * @param categoryName nome da categoria; as palavras dele não contam como palavras em comum
 * @param description  descrição interna do objeto; nula quando o funcionário não escreveu nenhuma
 */
public record FoundItemProfile(UUID categoryId, String categoryName, LocalDate foundOn, Place place,
                               String description) {

    public FoundItemProfile {
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(categoryName);
        Objects.requireNonNull(foundOn);
        Objects.requireNonNull(place);
    }
}
