package br.ucsal.cade.pickup.controller.summons.dto;

import br.ucsal.cade.pickup.api.PickupInstructions;
import br.ucsal.cade.pickup.persistence.summons.Summons;
import br.ucsal.cade.pickup.service.summons.IssuedSummons;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A convocação criada, com o que o estudante recebe (RF26). Só funcionários recebem esta resposta. */
@Schema(description = "Convocação criada a partir de uma sugestão, com as instruções que o estudante recebe.")
public record SummonsResponseDTO(
        UUID id,
        UUID suggestionId,
        UUID foundItemId,
        UUID lossReportId,
        @Schema(example = "ACTIVE") String status,
        @Schema(example = "Ativa") String statusLabel,
        Instant summonedAt,
        @Schema(description = "Até quando o objeto fica reservado (RN15).") Instant expiresAt,
        @Schema(example = "Sala de achados e perdidos") String place,
        @Schema(example = "Segunda a sexta, das 7h às 21h50") String hours,
        @Schema(example = "Um documento com foto (RG, CNH ou carteirinha da UCSal).") String whatToBring,
        @Schema(description = "Outras sugestões do mesmo objeto, postas em espera (RN18).") List<UUID> onHold) {

    public static SummonsResponseDTO from(IssuedSummons issued) {
        Summons summons = issued.summons();
        PickupInstructions instructions = issued.instructions();
        return new SummonsResponseDTO(summons.getId(), summons.getSuggestionId(), summons.getFoundItemId(),
                summons.getLossReportId(), summons.getStatus().code(), summons.getStatus().label(),
                summons.getSummonedAt(), summons.getExpiresAt(), instructions.place(), instructions.hours(),
                instructions.whatToBring(), issued.onHold());
    }
}
