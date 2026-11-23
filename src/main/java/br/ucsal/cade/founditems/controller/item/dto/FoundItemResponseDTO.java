package br.ucsal.cade.founditems.controller.item.dto;

import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.shared.category.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * O objeto com todos os dados, inclusive os reservados. Só as rotas de funcionário devolvem esta resposta (RN03).
 */
@Schema(description = "Objeto achado com todos os dados. Só funcionários recebem esta resposta (RN03).")
public record FoundItemResponseDTO(
        UUID id,
        UUID categoryId,
        @Schema(example = "Fone") String categoryName,
        @Schema(example = "Bloco 400") String block,
        @Schema(example = "Sala 421") String room,
        @Schema(example = "2026-11-04") LocalDate foundOn,
        @Schema(example = "Caixa Novembro/2026") String storageBox,
        @Schema(example = "MEDIUM") RiskLevel risk,
        @Schema(example = "Médio") String riskLabel,
        @Schema(example = "AVAILABLE") String status,
        @Schema(example = "Disponível") String statusLabel,
        String internalDescription,
        String reservedDetails,
        @Schema(description = "Caminho da foto na API; pedir com o token.", example = "/found-items/{id}/photo")
        String photoPath,
        Instant registeredAt,
        @Schema(description = "Quem trouxe o objeto (RF07); nulo se não foi registrado.", example = "Equipe de limpeza")
        String broughtBy,
        @Schema(description = "Se o objeto foi marcado como identificado (RF08).", example = "false")
        boolean identified,
        @Schema(description = "Nome encontrado no objeto; nulo se não há.") String identifiedName,
        @Schema(description = "E-mail encontrado no objeto; nulo se não há.") String identifiedEmail,
        @Schema(description = "Documento encontrado no objeto; nulo se não há.") String identifiedDocument,
        Instant identifiedAt,
        @Schema(description = "Versão do objeto; envie-a na correção (`PUT /found-items/{id}`), para a API recusar "
                + "uma correção feita sobre uma versão antiga.", example = "0")
        long version) {

    public static FoundItemResponseDTO from(FoundItem item) {
        return new FoundItemResponseDTO(item.getId(), item.getCategory().getId(), item.getCategory().getName(),
                item.getBlock(), item.getRoom(), item.getFoundOn(), item.getStorageBox(), item.getRisk(),
                item.getRisk().label(), item.getStatus().code(), item.getStatus().label(),
                item.getInternalDescription(), item.getReservedDetails(), "/found-items/" + item.getId() + "/photo",
                item.getRegisteredAt(), item.getBroughtBy(), item.isIdentified(), item.getIdentifiedName(),
                item.getIdentifiedEmail(), item.getIdentifiedDocument(), item.getIdentifiedAt(), item.getVersion());
    }
}
