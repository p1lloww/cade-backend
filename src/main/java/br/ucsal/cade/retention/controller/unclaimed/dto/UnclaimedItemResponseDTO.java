package br.ucsal.cade.retention.controller.unclaimed.dto;

import br.ucsal.cade.founditems.api.unclaimed.UnclaimedFoundItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Um objeto não reclamado. Só funcionários veem; a foto continua em `GET /found-items/{id}/photo`.")
public record UnclaimedItemResponseDTO(
        UUID id,
        String categoryName,
        String block,
        String room,
        LocalDate foundOn,
        @Schema(description = "Descrição interna do objeto; nula se não há.") String internalDescription,
        @Schema(description = "Quando o objeto entrou na sala; daí contam os 90 dias.") Instant registeredAt) {

    public static UnclaimedItemResponseDTO from(UnclaimedFoundItem item) {
        return new UnclaimedItemResponseDTO(item.id(), item.categoryName(), item.block(), item.room(), item.foundOn(),
                item.internalDescription(), item.registeredAt());
    }
}
