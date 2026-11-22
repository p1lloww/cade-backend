package br.ucsal.cade.pickup.api.event;

import java.util.List;
import java.util.UUID;

/**
 * Evento retirada.balcao.registrada: um objeto saiu pelo atalho de balcão, sem convocação (RF32). Publicado dentro da
 * transação; quem avisa os estudantes das sugestões encerradas (RF33, AVI-02) ouve depois do commit.
 *
 * @param closedSuggestions as sugestões do objeto que foram encerradas
 */
public record WalkInPickupRegisteredEvent(UUID walkInPickupId, UUID foundItemId, List<UUID> closedSuggestions) {

    public WalkInPickupRegisteredEvent {
        closedSuggestions = List.copyOf(closedSuggestions);
    }
}
