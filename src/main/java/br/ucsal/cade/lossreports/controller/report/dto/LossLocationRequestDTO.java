package br.ucsal.cade.lossreports.controller.report.dto;

import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Um lugar onde o estudante acha que perdeu o objeto. A sala é opcional.")
public record LossLocationRequestDTO(

        @Schema(description = "Bloco.", example = "Bloco 400", maxLength = LossLocation.MAX_LENGTH)
        @NotBlank(message = "Informe o bloco de cada local.")
        @Size(max = LossLocation.MAX_LENGTH, message = "O bloco pode ter no máximo 30 caracteres.")
        String block,

        @Schema(description = "Sala ou lugar dentro do bloco. Vazio quando o estudante não lembra.",
                example = "Sala 421", maxLength = LossLocation.MAX_LENGTH)
        @Size(max = LossLocation.MAX_LENGTH, message = "A sala pode ter no máximo 30 caracteres.")
        String room) {
}
