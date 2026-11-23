package br.ucsal.cade.retention.controller.unclaimed.dto;

import br.ucsal.cade.retention.persistence.disposal.Disposal;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "O destino registrado: para onde foi, quando e quem decidiu.")
public record DisposalResponseDTO(
        UUID foundItemId,
        @Schema(example = "BAZAAR") String destination,
        @Schema(example = "Bazar") String destinationLabel,
        @Schema(description = "Detalhe do destino; nulo se não há.") String note,
        Instant disposedAt,
        @Schema(description = "Id da conta do administrador.") UUID disposedBy) {

    public static DisposalResponseDTO from(Disposal disposal) {
        return new DisposalResponseDTO(disposal.getFoundItemId(), disposal.getDestination().name(),
                disposal.getDestination().label(), disposal.getNote(), disposal.getDisposedAt(),
                disposal.getDisposedBy());
    }
}
