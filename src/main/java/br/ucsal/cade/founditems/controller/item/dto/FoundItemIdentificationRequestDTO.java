package br.ucsal.cade.founditems.controller.item.dto;

import br.ucsal.cade.founditems.persistence.item.FoundItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = """
        O que foi encontrado no objeto que o identifica (RF08). Pelo menos um dos três. Só funcionários veem.""")
public record FoundItemIdentificationRequestDTO(

        @Schema(description = "Nome encontrado no objeto.", example = "Maria Souza", maxLength = FoundItem.MAX_NAME_LENGTH)
        @Size(max = FoundItem.MAX_NAME_LENGTH, message = "O nome pode ter no máximo 120 caracteres.")
        String name,

        @Schema(description = "E-mail encontrado no objeto.", example = "maria.souza@ucsal.edu.br",
                maxLength = FoundItem.MAX_EMAIL_LENGTH)
        @Email(message = "Informe um e-mail válido.")
        @Size(max = FoundItem.MAX_EMAIL_LENGTH, message = "O e-mail é longo demais.")
        String email,

        @Schema(description = "Documento encontrado no objeto (número, tipo).", example = "Carteirinha 202012345",
                maxLength = FoundItem.MAX_DOCUMENT_LENGTH)
        @Size(max = FoundItem.MAX_DOCUMENT_LENGTH, message = "O documento pode ter no máximo 60 caracteres.")
        String document) {
}
