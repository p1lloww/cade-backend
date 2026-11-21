package br.ucsal.cade.pickup.service.summons;

import br.ucsal.cade.pickup.api.PickupInstructions;
import br.ucsal.cade.pickup.persistence.summons.Summons;

import java.util.List;
import java.util.UUID;

/**
 * A convocação recém-criada, com as instruções que o estudante recebe.
 *
 * @param onHold as outras sugestões do mesmo objeto, postas em espera (RN18)
 */
public record IssuedSummons(Summons summons, PickupInstructions instructions, List<UUID> onHold) {

    public IssuedSummons {
        onHold = List.copyOf(onHold);
    }
}
