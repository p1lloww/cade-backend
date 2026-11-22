package br.ucsal.cade.pickup.controller.student.dto;

import br.ucsal.cade.pickup.api.PickupInstructions;
import br.ucsal.cade.pickup.service.summons.StudentSummons;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** Nada do objeto achado, nem o id: só onde, até quando e o que levar (RN03). */
@Schema(description = "Convocação ativa do estudante, com onde, até quando e o que levar para retirar o objeto.")
public record StudentSummonsResponseDTO(
        UUID id,
        @Schema(description = "O registro de perda convocado, para o front juntar com `GET /loss-reports/mine`.")
        UUID lossReportId,
        Instant summonedAt,
        @Schema(description = "Até quando o objeto fica reservado (RN15).") Instant expiresAt,
        @Schema(example = "Sala de achados e perdidos") String place,
        @Schema(example = "Segunda a sexta, das 7h às 21h50") String hours,
        @Schema(example = "Um documento com foto (RG, CNH ou carteirinha da UCSal).") String whatToBring) {

    public static StudentSummonsResponseDTO from(StudentSummons active) {
        PickupInstructions instructions = active.instructions();
        return new StudentSummonsResponseDTO(active.summons().getId(), active.summons().getLossReportId(),
                active.summons().getSummonedAt(), instructions.deadline(), instructions.place(), instructions.hours(),
                instructions.whatToBring());
    }
}
