package br.ucsal.cade.pickup.service.counter;

import br.ucsal.cade.pickup.persistence.summons.Summons;
import br.ucsal.cade.pickup.service.verification.VerificationRule;

/** A convocação como o funcionário a vê no balcão: com a regra do risco do objeto (RF28). */
public record SummonsAtCounter(Summons summons, VerificationRule rule) {
}
