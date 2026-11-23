package br.ucsal.cade.pickup.controller.student.dto;

import br.ucsal.cade.pickup.service.confirmation.StudentPickup;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Uma retirada registrada no balcão para o estudante e a confirmação dele. Não traz nada do objeto.")
public record StudentPickupResponseDTO(
        @Schema(description = "Id da retirada, para `POST /pickups/{id}/confirmation`.") UUID pickupId,
        @Schema(description = "Quando o funcionário registrou a retirada no balcão.") Instant handedOverAt,
        @Schema(description = "Se o estudante já confirmou no celular (RF30).", example = "false") boolean confirmed,
        @Schema(description = "Quando o estudante confirmou; nulo se ainda não confirmou.") Instant confirmedAt) {

    public static StudentPickupResponseDTO from(StudentPickup pickup) {
        return new StudentPickupResponseDTO(pickup.pickupId(), pickup.handedOverAt(), pickup.confirmed(),
                pickup.confirmedAt());
    }
}
