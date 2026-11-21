package br.ucsal.cade.matching.api;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A sugestão convocada.
 *
 * @param onHold as outras sugestões do mesmo objeto que foram postas em espera (RN18)
 */
public record SummonedSuggestion(SuggestionPair pair, List<UUID> onHold) {

    public SummonedSuggestion {
        Objects.requireNonNull(pair);
        onHold = List.copyOf(onHold);
    }
}
