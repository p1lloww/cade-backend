package br.ucsal.cade.lossreports.controller.reporter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Por que o estudante foi liberado; vai para o histórico.")
public record StudentReleaseRequestDTO(
        @NotBlank(message = "Informe o motivo da liberação.")
        @Size(max = 500, message = "O motivo pode ter no máximo 500 caracteres.")
        @Schema(example = "Conversou com a sala e explicou o engano.") String reason) {
}
