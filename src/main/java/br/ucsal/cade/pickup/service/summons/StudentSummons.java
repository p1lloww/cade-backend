package br.ucsal.cade.pickup.service.summons;

import br.ucsal.cade.pickup.api.PickupInstructions;
import br.ucsal.cade.pickup.persistence.summons.Summons;

/** Uma convocação Ativa do estudante, com o que ele precisa saber para retirar o objeto (RF26). */
public record StudentSummons(Summons summons, PickupInstructions instructions) {
}
