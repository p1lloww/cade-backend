package br.ucsal.cade.pickup.service.counter;

import br.ucsal.cade.lossreports.api.RecordedRefusal;
import br.ucsal.cade.pickup.persistence.summons.Summons;

import java.util.List;
import java.util.UUID;

/**
 * A convocação recusada no balcão.
 *
 * @param refusal as recusas do estudante depois desta (RN12)
 * @param resumed as sugestões do mesmo objeto que voltaram a Nova (RN18)
 */
public record RefusedSummons(Summons summons, RecordedRefusal refusal, List<UUID> resumed) {

    public RefusedSummons {
        resumed = List.copyOf(resumed);
    }
}
