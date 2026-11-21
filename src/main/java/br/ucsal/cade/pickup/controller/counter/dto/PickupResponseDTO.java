package br.ucsal.cade.pickup.controller.counter.dto;

import br.ucsal.cade.pickup.persistence.pickup.Pickup;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Retirada registrada no balcão.")
public record PickupResponseDTO(
        UUID id,
        UUID summonsId,
        UUID foundItemId,
        @Schema(example = "Carteirinha da UCSal") String documentType,
        @Schema(example = "Detalhe que não aparece na foto, ou o detalhe reservado") String verification,
        UUID handedOverBy,
        Instant handedOverAt) {

    public static PickupResponseDTO from(Pickup pickup) {
        return new PickupResponseDTO(pickup.getId(), pickup.getSummonsId(), pickup.getFoundItemId(),
                pickup.getCheck().documentType().label(), pickup.getCheck().verification().label(),
                pickup.getHandedOverBy(), pickup.getHandedOverAt());
    }
}
