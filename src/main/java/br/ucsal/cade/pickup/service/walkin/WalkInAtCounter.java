package br.ucsal.cade.pickup.service.walkin;

import br.ucsal.cade.founditems.api.WalkInFoundItem;
import br.ucsal.cade.pickup.service.verification.VerificationRule;

/** O objeto no atalho de balcão, com a regra do risco dele: o que conferir e as verificações que bastam (RF28). */
public record WalkInAtCounter(WalkInFoundItem item, VerificationRule rule) {
}
