package br.ucsal.cade.pickup.controller.counter.dto;

import br.ucsal.cade.pickup.persistence.summons.Summons;
import br.ucsal.cade.pickup.service.counter.RefusedSummons;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Convocação recusada no balcão.")
public record RefusalResponseDTO(
        UUID summonsId,
        @Schema(example = "REFUSED") String status,
        @Schema(example = "Recusada") String statusLabel,
        String reason,
        @Schema(description = "Recusas do estudante, contando esta (RN12).", example = "1") int studentRefusals,
        @Schema(description = "Se o estudante ficou impedido de abrir novos registros (3 recusas, RN12).")
        boolean studentBlocked,
        @Schema(description = "Sugestões do mesmo objeto que voltaram a Nova, para convocar a próxima (RN18).")
        List<UUID> resumed) {

    public static RefusalResponseDTO from(RefusedSummons refused) {
        Summons summons = refused.summons();
        return new RefusalResponseDTO(summons.getId(), summons.getStatus().code(), summons.getStatus().label(),
                summons.getRefusalReason(), refused.refusal().refusals(), refused.refusal().blocked(),
                refused.resumed());
    }
}
