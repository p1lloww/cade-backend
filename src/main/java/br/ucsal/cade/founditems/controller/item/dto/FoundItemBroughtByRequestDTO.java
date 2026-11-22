package br.ucsal.cade.founditems.controller.item.dto;

import br.ucsal.cade.founditems.persistence.item.FoundItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Quem trouxe o objeto à sala (RF07).")
public record FoundItemBroughtByRequestDTO(

        @Schema(description = "Nome e matrícula de quem trouxe, ou \"equipe de limpeza\". Vazio apaga.",
                example = "Maria Souza (202012345)", maxLength = FoundItem.MAX_BROUGHT_BY_LENGTH)
        @Size(max = FoundItem.MAX_BROUGHT_BY_LENGTH, message = "Quem trouxe pode ter no máximo 120 caracteres.")
        String broughtBy) {
}
