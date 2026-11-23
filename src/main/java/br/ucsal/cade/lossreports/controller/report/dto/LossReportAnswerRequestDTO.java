package br.ucsal.cade.lossreports.controller.report.dto;

import br.ucsal.cade.lossreports.persistence.report.LossReport;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Resposta do estudante ao pedido de mais informações do funcionário.")
public record LossReportAnswerRequestDTO(

        @Schema(description = "O que o funcionário pediu para saber.", example = "A capa é azul e tem um risco na tampa.",
                maxLength = LossReport.MAX_ANSWER_LENGTH)
        @NotBlank(message = "Escreva a resposta.")
        @Size(max = LossReport.MAX_ANSWER_LENGTH, message = "A resposta pode ter no máximo 1000 caracteres.")
        String answer) {
}
