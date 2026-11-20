package br.ucsal.cade.access.dto.request;

import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.security.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Pedido do administrador para dar ({@code STAFF}) ou retirar ({@code STUDENT}) o perfil de funcionário (RF03). O
 * motivo vai para o histórico (RF43).
 */
public record RoleChangeRequestDTO(
        @NotNull(message = "Informe o perfil.")
        Role role,

        @NotBlank(message = "Informe o motivo.")
        @Size(max = AuditEvent.MAX_REASON_LENGTH,
                message = "O motivo pode ter no máximo " + AuditEvent.MAX_REASON_LENGTH + " caracteres.")
        String reason) {

    /** O perfil de administrador só é dado na instalação. */
    @AssertTrue(message = "O perfil deve ser STAFF ou STUDENT.")
    public boolean isAssignableRole() {
        return role != Role.ADMIN;
    }
}
