package br.ucsal.cade.pickup.api.event;

import java.util.List;
import java.util.UUID;

/**
 * Com a retirada, as outras sugestões do objeto foram encerradas (RF33). Publicado na transação da retirada, só quando
 * havia alguma: os estudantes dessas sugestões são avisados pela caixa de saída nessa mesma transação (D30).
 */
public record OtherSuggestionsClosedEvent(UUID foundItemId, List<Closed> closed) {

    public OtherSuggestionsClosedEvent {
        closed = List.copyOf(closed);
    }

    /** Uma sugestão encerrada e o registro de perda que ela sugeria. */
    public record Closed(UUID suggestionId, UUID lossReportId) {
    }
}
