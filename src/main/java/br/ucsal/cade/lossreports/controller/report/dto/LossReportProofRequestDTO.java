package br.ucsal.cade.lossreports.controller.report.dto;

import br.ucsal.cade.lossreports.persistence.report.LossReport;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "A parte de texto da prova. A foto vai na parte `file`.")
public record LossReportProofRequestDTO(

        @Schema(description = "Número de série do objeto. Vazio mantém o que já estava.", example = "SN-123456",
                maxLength = LossReport.MAX_SERIAL_NUMBER_LENGTH)
        @Size(max = LossReport.MAX_SERIAL_NUMBER_LENGTH,
                message = "O número de série pode ter no máximo 60 caracteres.")
        String serialNumber) {
}
