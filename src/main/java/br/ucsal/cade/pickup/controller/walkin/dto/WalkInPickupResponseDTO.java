package br.ucsal.cade.pickup.controller.walkin.dto;

import br.ucsal.cade.pickup.persistence.walkin.WalkInPickup;
import br.ucsal.cade.pickup.service.walkin.RegisteredWalkIn;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Retirada registrada pelo atalho de balcão.")
public record WalkInPickupResponseDTO(
        UUID id,
        UUID foundItemId,
        @Schema(example = "STUDENT") String holderType,
        @Schema(example = "Estudante") String holderTypeLabel,
        @Schema(example = "Nome de quem retira") String holderName,
        @Schema(example = "RG") String documentType,
        @Schema(example = "Descrição coerente com o objeto") String verification,
        UUID handedOverBy,
        Instant handedOverAt,
        @Schema(description = "Sugestões do objeto encerradas porque ele saiu da sala (RF33).")
        List<UUID> closedSuggestions) {

    public static WalkInPickupResponseDTO from(RegisteredWalkIn registered) {
        WalkInPickup pickup = registered.pickup();
        return new WalkInPickupResponseDTO(pickup.getId(), pickup.getFoundItemId(), pickup.getHolder().type().name(),
                pickup.getHolder().type().label(), pickup.getHolder().name(),
                pickup.getCheck().documentType().label(), pickup.getCheck().verification().label(),
                pickup.getHandedOverBy(), pickup.getHandedOverAt(), registered.closedSuggestions());
    }
}
