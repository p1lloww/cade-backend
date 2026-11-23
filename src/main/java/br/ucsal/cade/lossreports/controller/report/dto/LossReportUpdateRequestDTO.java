package br.ucsal.cade.lossreports.controller.report.dto;

import br.ucsal.cade.lossreports.persistence.period.Shift;
import br.ucsal.cade.lossreports.persistence.report.LossReport;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = """
        Correção de um registro de perda. Leva os mesmos campos do registro (a foto não muda) e a `version` que o \
        estudante recebeu por último.""")
public record LossReportUpdateRequestDTO(

        @Schema(description = "Id da categoria (GET /categories).")
        @NotNull(message = "Escolha a categoria.")
        UUID categoryId,

        @Schema(description = "Como é o objeto: cor, marca, material, o que tiver.", example = "Fone JBL preto",
                maxLength = LossReport.MAX_TEXT_LENGTH)
        @NotBlank(message = "Descreva o objeto.")
        @Size(max = LossReport.MAX_TEXT_LENGTH, message = "A descrição pode ter no máximo 500 caracteres.")
        String description,

        @Schema(description = "Detalhe que só o dono conhece. Vazio apaga o detalhe.",
                example = "Adesivo de estrela no lado esquerdo", maxLength = LossReport.MAX_TEXT_LENGTH)
        @Size(max = LossReport.MAX_TEXT_LENGTH, message = "O detalhe pode ter no máximo 500 caracteres.")
        String secretDetail,

        @Schema(description = "Onde acha que perdeu, até 5 locais. Vazio quando não sabe.")
        @Size(max = LossReport.MAX_LOCATIONS, message = "Informe no máximo 5 locais.")
        List<@NotNull(message = "Informe o local.") @Valid LossLocationRequestDTO> locations,

        @Schema(description = "Data em que perdeu, ou o início do período.", example = "2026-11-04")
        @NotNull(message = "Informe quando perdeu o objeto.")
        LocalDate lostFrom,

        @Schema(description = "Fim do período, até 30 dias depois do início. Vazio quando é uma data só.",
                example = "2026-11-05")
        LocalDate lostUntil,

        @Schema(description = "Turno. Vazio quando não lembra.")
        Shift shift,

        @Schema(description = "A `version` do registro na última resposta que o estudante recebeu. Se o registro mudou "
                + "desde então (por exemplo, foi convocado), a correção é recusada com 409.", example = "0")
        @NotNull(message = "Informe a versão do registro.")
        Long version) {
}
