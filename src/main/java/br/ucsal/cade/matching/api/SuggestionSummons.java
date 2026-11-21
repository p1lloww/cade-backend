package br.ucsal.cade.matching.api;

import java.util.UUID;

/** A decisão de convocar a partir de uma sugestão (RF24, RF26), chamada pelo módulo de convocação e retirada. */
public interface SuggestionSummons {

    /**
     * O objeto e a perda da sugestão, em qualquer situação, para a convocação reservar o objeto antes de mexer nas
     * sugestões. Só roda dentro da transação da convocação.
     *
     * @throws br.ucsal.cade.shared.exception.NotFoundException se a sugestão não existe
     */
    SuggestionPair pairOf(UUID suggestionId);

    /**
     * Nova → Convocada, e as outras sugestões Novas do mesmo objeto → Em espera (RN18), cada mudança no histórico
     * (RF43). Só roda dentro da transação da convocação (seção 7.1).
     *
     * @param staffId id do funcionário que convoca
     * @throws br.ucsal.cade.shared.exception.NotFoundException se a sugestão não existe
     * @throws br.ucsal.cade.shared.exception.InvalidStatusTransitionException se a sugestão não está Nova
     */
    SummonedSuggestion summon(UUID suggestionId, UUID staffId);
}
