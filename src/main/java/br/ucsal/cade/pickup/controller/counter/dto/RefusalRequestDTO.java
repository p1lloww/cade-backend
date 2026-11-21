package br.ucsal.cade.pickup.controller.counter.dto;

import br.ucsal.cade.shared.audit.AuditEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O motivo da recusa vai para a convocação e para o histórico (RF31, RF43). */
public record RefusalRequestDTO(
        @NotBlank(message = "Informe o motivo.")
        @Size(max = AuditEvent.MAX_REASON_LENGTH,
                message = "O motivo pode ter no máximo " + AuditEvent.MAX_REASON_LENGTH + " caracteres.")
        String reason) {
}
