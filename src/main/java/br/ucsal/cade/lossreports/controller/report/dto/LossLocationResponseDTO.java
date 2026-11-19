package br.ucsal.cade.lossreports.controller.report.dto;

import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Um local informado pelo estudante.")
public record LossLocationResponseDTO(

        @Schema(example = "Bloco 400")
        String block,

        @Schema(description = "Nulo quando o estudante não informou.", example = "Sala 421")
        String room,

        @Schema(description = "Texto pronto para mostrar.", example = "Bloco 400, Sala 421")
        String label) {

    static final String UNKNOWN_ROOM = "sala não informada";

    public static LossLocationResponseDTO from(LossLocation location) {
        String room = location.room() == null ? UNKNOWN_ROOM : location.room();
        return new LossLocationResponseDTO(location.block(), location.room(), location.block() + ", " + room);
    }
}
