package br.ucsal.cade.administration.controller.settings.dto;

import br.ucsal.cade.shared.settings.DeadlineValues;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Os prazos em vigor, em dias.")
public record DeadlineSettingsResponseDTO(
        @Schema(example = "90") int storageDays,
        @Schema(example = "7") int summonsDays,
        @Schema(example = "30") int reportValidityDays) {

    public static DeadlineSettingsResponseDTO from(DeadlineValues values) {
        return new DeadlineSettingsResponseDTO(values.storageDays(), values.summonsDays(),
                values.reportValidityDays());
    }
}
