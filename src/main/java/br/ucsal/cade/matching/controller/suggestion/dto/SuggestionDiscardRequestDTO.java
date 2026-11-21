package br.ucsal.cade.matching.controller.suggestion.dto;

import br.ucsal.cade.shared.audit.AuditEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O motivo do descarte vai para o histórico (RF24, RF43). */
public record SuggestionDiscardRequestDTO(
        @NotBlank(message = "Informe o motivo.")
        @Size(max = AuditEvent.MAX_REASON_LENGTH,
                message = "O motivo pode ter no máximo " + AuditEvent.MAX_REASON_LENGTH + " caracteres.")
        String reason) {
}
