package br.ucsal.cade.pickup.service.confirmation;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma retirada vista pelo estudante, com a confirmação dele. Não leva nada do objeto nem do funcionário (RN03).
 *
 * @param confirmedAt quando o estudante confirmou; nulo se ainda não confirmou
 */
public record StudentPickup(UUID pickupId, Instant handedOverAt, Instant confirmedAt) {

    public boolean confirmed() {
        return confirmedAt != null;
    }
}
