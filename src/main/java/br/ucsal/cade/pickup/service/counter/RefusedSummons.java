package br.ucsal.cade.pickup.service.counter;

import br.ucsal.cade.lossreports.api.RecordedRefusal;
import br.ucsal.cade.matching.api.RefusedSuggestion;
import br.ucsal.cade.pickup.persistence.summons.Summons;

/**
 * A convocação recusada no balcão.
 *
 * @param refusal    as recusas do estudante depois desta (RN12)
 * @param suggestion as sugestões que voltaram a Nova (RN18) e as da perda que foram encerradas (D26)
 */
public record RefusedSummons(Summons summons, RecordedRefusal refusal, RefusedSuggestion suggestion) {
}
