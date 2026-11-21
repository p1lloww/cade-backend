package br.ucsal.cade.access.service.user;

import br.ucsal.cade.shared.security.Role;

import java.util.Objects;

/**
 * Pedido do administrador para dar ({@code STAFF}) ou retirar ({@code STUDENT}) o perfil de funcionário (RF03).
 *
 * @param reason motivo, que vai para o histórico (RF43)
 */
public record ChangeRole(Role role, String reason) {

    public ChangeRole {
        Objects.requireNonNull(role);
        Objects.requireNonNull(reason);
    }
}
