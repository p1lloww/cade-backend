package br.ucsal.cade.matching.api;

import java.util.List;
import java.util.UUID;

/**
 * A sugestão recusada no balcão e o que mudou à volta dela.
 *
 * @param resumed as sugestões Em espera do mesmo objeto, que voltaram a Nova (RN18)
 * @param closed  as outras sugestões da mesma perda, encerradas porque a perda se encerrou (D26)
 */
public record RefusedSuggestion(List<UUID> resumed, List<UUID> closed) {

    public RefusedSuggestion {
        resumed = List.copyOf(resumed);
        closed = List.copyOf(closed);
    }
}
