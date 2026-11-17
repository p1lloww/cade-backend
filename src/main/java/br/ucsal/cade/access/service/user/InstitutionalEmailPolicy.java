package br.ucsal.cade.access.service.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Só e-mails institucionais podem ter conta (requisitos, seção 2; evidência E4). O domínio vem de
 * {@code cade.allowed-email-domain} e é sempre conferido no back-end, nunca só no front-end.
 */
@Component
public class InstitutionalEmailPolicy {

    private final String domain;

    public InstitutionalEmailPolicy(@Value("${cade.allowed-email-domain}") String domain) {
        this.domain = Objects.requireNonNull(domain);
    }

    /**
     * Se um e-mail normalizado ({@link br.ucsal.cade.access.persistence.user.User#normalize(String)}) pertence ao
     * domínio institucional. Exige exatamente um {@code @}, para recusar {@code a@b@ucsal.edu.br}.
     */
    public boolean accepts(String normalizedEmail) {
        int at = normalizedEmail.indexOf('@');
        return at > 0
                && at == normalizedEmail.lastIndexOf('@')
                && normalizedEmail.substring(at + 1).equals(domain);
    }

    public String domain() {
        return domain;
    }
}
