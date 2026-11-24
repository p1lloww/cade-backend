package br.ucsal.cade.lossreports.api.candidate;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * O que o cruzamento usa de um registro de perda (RN07). Não leva o detalhe secreto, a foto nem o estudante (RF14).
 *
 * @param locations vazia quando o estudante não sabe onde perdeu
 */
public record MatchableLossReport(UUID id, UUID categoryId, LocalDate lostFrom, LocalDate lostUntil,
                                  List<Location> locations, String description) {

    public MatchableLossReport {
        Objects.requireNonNull(id);
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(lostFrom);
        Objects.requireNonNull(lostUntil);
        Objects.requireNonNull(description);
        locations = locations == null ? List.of() : List.copyOf(locations);
    }

    /** @param room nula quando o estudante não informou a sala */
    public record Location(String block, String room) {

        public Location {
            Objects.requireNonNull(block);
        }
    }
}
