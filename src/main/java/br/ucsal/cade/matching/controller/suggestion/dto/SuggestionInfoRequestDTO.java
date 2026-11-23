package br.ucsal.cade.matching.controller.suggestion.dto;

import br.ucsal.cade.shared.audit.AuditEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O que o funcionário precisa saber; vai ao estudante por e-mail e ao histórico (RF24, RF35, RF43). */
@Schema(description = """
        O que o funcionário precisa que o estudante responda. O texto é enviado ao estudante, então não cite o objeto \
        achado nem nenhum dado dele (RN03).""")
public record SuggestionInfoRequestDTO(
        @Schema(description = "A pergunta ao estudante.", example = "A capa do celular tem alguma marca? Qual?")
        @NotBlank(message = "Informe o que você precisa saber.")
        @Size(max = AuditEvent.MAX_REASON_LENGTH,
                message = "O pedido pode ter no máximo " + AuditEvent.MAX_REASON_LENGTH + " caracteres.")
        String reason) {
}
