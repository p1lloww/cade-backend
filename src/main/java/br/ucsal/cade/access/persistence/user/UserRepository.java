package br.ucsal.cade.access.persistence.user;

import br.ucsal.cade.shared.security.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Busca por um e-mail já normalizado ({@link User#normalize(String)}). */
    Optional<User> findByEmail(String email);

    /**
     * Contas com o perfil informado e cujo e-mail contém o trecho informado. Perfil nulo não filtra; trecho vazio
     * também não. O trecho precisa vir normalizado, como os e-mails gravados.
     */
    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and u.email like concat('%', :emailPart, '%')""")
    Page<User> search(@Param("role") Role role, @Param("emailPart") String emailPart, Pageable pageable);
}
