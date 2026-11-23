package br.ucsal.cade.administration.controller.settings.dto;

import br.ucsal.cade.shared.settings.DeadlineValues;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Os três prazos, em dias inteiros (RF45). Os limites estão na descrição da rota.")
public record DeadlineSettingsRequestDTO(

        @Schema(description = "Guarda até o objeto virar não reclamado (RN16).", example = "90")
        @NotNull(message = "Informe o prazo de guarda.")
        Integer storageDays,

        @Schema(description = "Retirada depois da convocação (RN15).", example = "7")
        @NotNull(message = "Informe o prazo de retirada.")
        Integer summonsDays,

        @Schema(description = "Validade do registro de perda (RN14).", example = "30")
        @NotNull(message = "Informe a validade do registro.")
        Integer reportValidityDays) {

    public DeadlineValues toValues() {
        return new DeadlineValues(storageDays, summonsDays, reportValidityDays);
    }
}
