package br.ucsal.cade.access.api;

import java.util.Optional;
import java.util.UUID;

/** O e-mail institucional de uma conta, para os avisos por e-mail (RF34, RF35). */
public interface UserEmails {

    /** Vazio se a conta não existe. */
    Optional<String> emailOf(UUID userId);
}
