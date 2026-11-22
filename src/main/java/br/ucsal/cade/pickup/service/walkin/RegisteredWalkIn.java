package br.ucsal.cade.pickup.service.walkin;

import br.ucsal.cade.pickup.persistence.walkin.WalkInPickup;

import java.util.List;
import java.util.UUID;

/**
 * A retirada pelo atalho, já gravada.
 *
 * @param closedSuggestions as sugestões do objeto encerradas porque ele saiu da sala (RF33)
 */
public record RegisteredWalkIn(WalkInPickup pickup, List<UUID> closedSuggestions) {

    public RegisteredWalkIn {
        closedSuggestions = List.copyOf(closedSuggestions);
    }
}
