package br.ucsal.cade.lossreports.controller.reporter.dto;

import br.ucsal.cade.lossreports.persistence.reporter.LossReporter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Só o id e as recusas: o e-mail da conta fica no módulo de acesso (D10), na lista de usuários do administrador. */
@Schema(description = "Estudante e quantas retiradas dele foram recusadas no balcão.")
public record BlockedStudentResponseDTO(
        @Schema(description = "Id da conta; o e-mail está em `GET /users`.") UUID studentId,
        @Schema(example = "3") int refusals,
        @Schema(description = "Se o estudante está impedido de registrar perdas (3 recusas ou mais, RN12).")
        boolean blocked) {

    public static BlockedStudentResponseDTO from(LossReporter reporter) {
        return new BlockedStudentResponseDTO(reporter.getStudentId(), reporter.getRefusals(), reporter.isBlocked());
    }
}
