package br.ucsal.cade.access.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Busca por um e-mail já normalizado ({@link User#normalize(String)}). */
    Optional<User> findByEmail(String email);
}
