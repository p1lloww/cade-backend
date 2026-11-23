package br.ucsal.cade.matching.api;

import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import br.ucsal.cade.shared.exception.NotFoundException;

import java.util.UUID;

/**
 * A decisão de convocar a partir de uma sugestão (RF24, RF26) e o fim da convocação no balcão, chamados pelo módulo de
 * convocação e retirada.
 */
public interface SuggestionSummons {

    /**
     * O objeto e a perda da sugestão, em qualquer situação, para a convocação reservar o objeto antes de mexer nas
     * sugestões. Só roda dentro da transação da convocação.
     *
     * @throws NotFoundException se a sugestão não existe
     */
    SuggestionPair pairOf(UUID suggestionId);

    /**
     * Nova → Convocada, e as outras sugestões Novas do mesmo objeto → Em espera (RN18), cada mudança no histórico
     * (RF43). Só roda dentro da transação da convocação (seção 7.1).
     *
     * @param staffId id do funcionário que convoca
     * @throws NotFoundException se a sugestão não existe
     * @throws InvalidStatusTransitionException se a sugestão não está Nova
     */
    SummonedSuggestion summon(UUID suggestionId, UUID staffId);

    /**
     * Convocada → Concluída: o estudante retirou o objeto (RF29), com o registro no histórico (RF43). As outras
     * sugestões Novas ou Em espera da mesma perda são encerradas, porque a perda se conclui (D26). As outras sugestões
     * do objeto continuam como estão; encerrá-las é o RF33. Só roda dentro da transação da retirada.
     *
     * @throws NotFoundException se a sugestão não existe
     * @throws InvalidStatusTransitionException se a sugestão não está Convocada
     */
    void complete(UUID suggestionId, UUID staffId);

    /**
     * Convocada → Recusada: a verificação falhou no balcão (RF31). As sugestões Em espera do mesmo objeto voltam a
     * Nova (RN18), para o funcionário convocar a próxima. As outras sugestões Novas ou Em espera da mesma perda são
     * encerradas, porque a perda se encerra com a recusa (D26). Cada mudança vai para o histórico (RF43). Só roda
     * dentro da transação da recusa.
     *
     * @throws NotFoundException se a sugestão não existe
     * @throws InvalidStatusTransitionException se a sugestão não está Convocada
     */
    RefusedSuggestion refuse(UUID suggestionId, UUID staffId);

    /**
     * Convocada → Vencida: o prazo da retirada passou (RF27). As sugestões Em espera do mesmo objeto voltam a Nova
     * (RN18), para o funcionário convocar a próxima. Diferente da recusa, a perda não se encerra: ela volta a Aberto, e
     * as outras sugestões dela continuam como estão. Cada mudança vai para o histórico (RF43). Só roda dentro da
     * transação de quem libera.
     *
     * @param actorId id do funcionário, ou {@code AuditEvent.SYSTEM_ACTOR} numa tarefa agendada
     * @throws NotFoundException se a sugestão não existe
     * @throws InvalidStatusTransitionException se a sugestão não está Convocada
     */
    ExpiredSuggestion expire(UUID suggestionId, UUID actorId);
}
