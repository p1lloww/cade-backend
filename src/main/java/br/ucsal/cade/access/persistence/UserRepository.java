package br.ucsal.cade.access.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Looks up by an already normalized e-mail ({@link User#normalize(String)}). */
    Optional<User> findByEmail(String email);
}
