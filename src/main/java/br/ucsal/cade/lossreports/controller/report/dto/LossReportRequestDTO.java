package br.ucsal.cade.lossreports.controller.report.dto;

import br.ucsal.cade.lossreports.persistence.period.Shift;
import br.ucsal.cade.lossreports.persistence.report.LossReport;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Registro de uma perda (RF13, RF14). Só categoria, descrição e data são obrigatórias; regras que dependem do relógio
 * ou dos outros registros (data futura, período, limite de 5) ficam no domínio.
 */
@Schema(description = "Registro de uma perda. Só categoria, descrição e data são obrigatórias.")
public record LossReportRequestDTO(

        @Schema(description = "Id da categoria (GET /categories).")
        @NotNull(message = "Escolha a categoria.")
        UUID categoryId,

        @Schema(description = "Como é o objeto: cor, marca, material, o que tiver.", example = "Fone JBL preto",
                maxLength = LossReport.MAX_TEXT_LENGTH)
        @NotBlank(message = "Descreva o objeto.")
        @Size(max = LossReport.MAX_TEXT_LENGTH, message = "A descrição pode ter no máximo 500 caracteres.")
        String description,

        @Schema(description = """
                Detalhe que só o dono conhece (adesivo, arranhão, nome gravado). Nunca é mostrado a outros \
                estudantes (RF14).""", example = "Adesivo de estrela no lado esquerdo",
                maxLength = LossReport.MAX_TEXT_LENGTH)
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
        Shift shift) {

    public RegisterLossReport toRegisterLossReport() {
        List<LossLocationRequestDTO> places = locations == null ? List.of() : locations;
        return new RegisterLossReport(categoryId, description, secretDetail,
                places.stream().map(LossLocationRequestDTO::toLocation).toList(), lostFrom, lostUntil, shift);
    }
}
