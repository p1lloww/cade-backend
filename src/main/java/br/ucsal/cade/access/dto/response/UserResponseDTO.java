package br.ucsal.cade.access.dto.response;

import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.shared.security.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponseDTO(UUID id, String email, Role role, Instant createdAt) {

    public static UserResponseDTO from(User user) {
        return new UserResponseDTO(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
