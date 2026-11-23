package br.ucsal.cade.retention.controller.unclaimed.dto;

import br.ucsal.cade.retention.persistence.disposal.Disposal;
import br.ucsal.cade.retention.persistence.disposal.DisposalDestination;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "O destino de um objeto não reclamado (RF38).")
public record DisposalRequestDTO(

        @Schema(description = "Para onde vai o objeto. Sem valor, vale o bazar.", example = "BAZAAR",
                allowableValues = {"BAZAAR", "DISCARD", "OTHER"})
        DisposalDestination destination,

        @Schema(description = "Detalhe do destino (obrigatório quando é OTHER).", example = "Doado ao projeto social",
                maxLength = Disposal.MAX_NOTE_LENGTH)
        @Size(max = Disposal.MAX_NOTE_LENGTH, message = "A observação pode ter no máximo 200 caracteres.")
        String note) {
}
