package br.ucsal.cade.access.api;

import br.ucsal.cade.shared.security.Role;

import java.time.Duration;
import java.util.UUID;

/**
 * Uma conta autenticada e o token dela.
 *
 * @param userId    id da conta (claim {@code sub} do token)
 * @param email     e-mail da conta
 * @param role      perfil que a conta tem
 * @param token     o JWT, enviado como {@code Authorization: Bearer <token>}
 * @param expiresIn por quanto tempo o token vale
 */
public record SignedIn(UUID userId, String email, Role role, String token, Duration expiresIn) {

    @Override
    public String toString() {
        return "SignedIn[userId=" + userId + ", email=" + email + ", role=" + role + ", token=******]";
    }
}
