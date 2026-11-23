package br.ucsal.cade.matching.api;

import java.util.List;
import java.util.UUID;

/** As sugestões de um objeto que saiu da sala, chamado pelo módulo de retirada. */
public interface FoundItemSuggestions {

    /**
     * Nova ou Em espera → Encerrada, para todas as sugestões do objeto, cada uma no histórico (RF43): o objeto foi
     * entregue e não pode mais ser convocado (RF33). Só roda dentro da transação da retirada.
     *
     * @return os ids das sugestões encerradas
     */
    List<UUID> closeOpenOf(UUID foundItemId, UUID staffId);

    /**
     * Nova ou Em espera → Encerrada, para todas as sugestões do objeto que acabou de ser retirado por uma
     * convocação (RF33), cada uma no histórico (RF43). A sugestão convocada já está Concluída e não entra. Devolve também o
     * registro de perda de cada uma, para os estudantes serem avisados. Só roda dentro da transação da retirada.
     */
    List<ClosedSuggestion> closeOthersOf(UUID foundItemId, UUID staffId);
}
