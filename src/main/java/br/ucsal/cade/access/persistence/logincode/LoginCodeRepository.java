package br.ucsal.cade.access.persistence.logincode;

import org.springframework.data.jpa.repository.JpaRepository;
import br.ucsal.cade.access.persistence.user.User;
import java.util.Optional;
import java.util.UUID;

public interface LoginCodeRepository extends JpaRepository<LoginCode, UUID> {

    /**
     * The latest code of an already normalized e-mail
     * ({@link User#normalize(String)}); the only one still valid.
     */
    Optional<LoginCode> findFirstByEmailOrderByCreatedAtDesc(String email);
}
