package br.ucsal.cade.matching.api;

import java.util.List;
import java.util.UUID;

/**
 * A sugestão cuja convocação venceu e o que mudou à volta dela.
 *
 * @param resumed as sugestões Em espera do mesmo objeto, que voltaram a Nova (RN18)
 */
public record ExpiredSuggestion(List<UUID> resumed) {

    public ExpiredSuggestion {
        resumed = List.copyOf(resumed);
    }
}
