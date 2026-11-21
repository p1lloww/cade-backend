package br.ucsal.cade.matching.service.score;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * O que o cruzamento usa de um registro de perda. Uma data só é um período com início igual ao fim.
 *
 * @param places vazia quando o estudante não sabe onde perdeu
 */
public record LossReportProfile(UUID categoryId, LocalDate lostFrom, LocalDate lostUntil, List<Place> places,
                                String description) {

    public LossReportProfile {
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(lostFrom);
        Objects.requireNonNull(lostUntil);
        Objects.requireNonNull(description);
        places = places == null ? List.of() : List.copyOf(places);
    }
}
