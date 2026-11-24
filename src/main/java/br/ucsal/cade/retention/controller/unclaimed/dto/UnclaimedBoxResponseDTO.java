package br.ucsal.cade.retention.controller.unclaimed.dto;

import br.ucsal.cade.retention.service.disposal.UnclaimedItemsByBox;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Os não reclamados de uma caixa de mês, para retirar da sala de uma vez (RF39).")
public record UnclaimedBoxResponseDTO(
        @Schema(example = "Caixa Agosto/2026") String storageBox,
        List<UnclaimedItemResponseDTO> items) {

    public static UnclaimedBoxResponseDTO from(UnclaimedItemsByBox box) {
        return new UnclaimedBoxResponseDTO(box.storageBox(),
                box.items().stream().map(UnclaimedItemResponseDTO::from).toList());
    }
}
